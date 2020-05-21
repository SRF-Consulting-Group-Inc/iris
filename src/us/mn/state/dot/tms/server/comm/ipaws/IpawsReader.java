/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server.comm.ipaws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.sonar.SonarException;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.IncidentHelper;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertHelper;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.server.IpawsAlertImpl;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.Json;

/**
 * Integrated Public Alert and Warning System (IPAWS) alert reader. Reads and
 * parses IPAWS CAP XMLs into IpawsAlert objects and saves them to the
 * database. 
 *
 * @author Michael Janson
 * @author Gordon Parikh
 */

public class IpawsReader {
	
	/** Date formatters */
	// 2020-05-12T21:59:23-00:00
	private static final String dtFormat = "yyyy-MM-dd'T'HH:mm:ssX";
	private static final SimpleDateFormat dtFormatter =
			new SimpleDateFormat(dtFormat);
	
//	/** Cache of IPAWS alert proxy objects */
//	private final TypeCache<IpawsAlert> cache;

	/** Read alerts from a directory containing XML files. */
    public static void readIpaws(String dirPath) throws IOException {
        File folder = new File(dirPath);

        File[] files = folder.listFiles();
        
        DocumentBuilderFactory dbFactory =
        		DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
        	dBuilder = dbFactory.newDocumentBuilder();
		    for (File xmlFile : files) {
		        	InputStream in = new GZIPInputStream(
		        			new FileInputStream(xmlFile));
		            Document doc = dBuilder.parse(in);
		            doc.getDocumentElement().normalize();
		
		            NodeList nodeList = doc.getElementsByTagName("alert");
		            try {
			            for (int i = 0; i < nodeList.getLength(); i++) {
			                getIpawsAlert(nodeList.item(i));
			            }
		            }catch(SonarException e) {
//		        		if (e.getMessage().contains(
//		        				"Name already exists"))
//		        			continue;
//		        		else throw new SonarException(e);
		            }
		    }
        }catch(ParserConfigurationException | SAXException
        		| ParseException | TMSException e) {
        	e.printStackTrace();
        }
    }
    
    /** Read alerts from an InputStream (live feed). */
    public static void readIpaws(InputStream is) throws IOException {
    	// make a copy of the input stream - if we hit an exception we will
    	// save the XML and the text of the exception on the server
    	// TODO make these controllable with a system attribute
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	byte[] buf = new byte[1024];
    	int len;
    	while ((len = is.read(buf)) > -1)
    		baos.write(buf, 0, len);
    	baos.flush();
    	InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
    	
        DocumentBuilderFactory dbFactory =
        		DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
        	dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is1);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("alert");
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                getIpawsAlert(nodeList.item(i));
	            }
         } catch(ParserConfigurationException | SAXException | ParseException
        		| TMSException | SonarException e) {
        	System.out.println("Hit exception: " + e.getClass().getName());
//        	e.printStackTrace();
        	
        	// save the XML contents to a file
        	DateTimeFormatter dtf = DateTimeFormatter.ofPattern(
        			"yyyyMMdd-HHmmss");
        	String dts = dtf.format(LocalDateTime.now());
        	String fn = String.format(
        			"/var/log/iris/IpawsAlert_err_%s.xml", dts);
        	OutputStream xmlos = new FileOutputStream(fn);
        	baos.writeTo(xmlos);
        	
        	// and the exception too (ipaws_alert_<date>_exc.log)
        	String excfn = String.format(
        			"/var/log/iris/IpawsAlert_err_%s_exc.log", dts);
        	PrintWriter pw = new PrintWriter(new FileWriter(excfn));
        	e.printStackTrace(pw);
        	System.out.println(String.format("See %s for details", excfn));
        }
    }
    
    private static void getIpawsAlert(Node node)
    		throws ParseException, SonarException, TMSException {

//    	System.out.println("In IpawsReader.getIpawsAlert");
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            
            // check if the alert exists
            String n = getTagValue("identifier", element);
            String name = n;
            IpawsAlertImpl ia =
            		(IpawsAlertImpl) IpawsAlertHelper.lookup(name);
            
            // if it doesn't, create a new one
            boolean newObj = false;
            if (ia == null) {
            	System.out.println("Creating new alert with name: " + name);
            	ia = new IpawsAlertImpl(name);
            	newObj = true;
            } else
            	System.out.println("Updating alert with name: " + name);
            
            // either way set all the values
            ia.setIdentifier(getTagValue("identifier", element));
            ia.setSender(getTagValue("sender", element));
            ia.setSentDate(parseDate(getTagValue("sent", element)));
            ia.setStatus(getTagValue("status", element));
            ia.setMsgType(getTagValue("msgType", element));
            ia.setScope(getTagValue("scope", element));
            ia.setCodes(getTagValueArray("code", element));
            ia.setNote(getTagValue("note", element));
            ia.setAlertReferences(getTagValueArray("references", element));
            ia.setIncidents(getTagValueArray("incidents", element));
            ia.setCategories(getTagValueArray("category", element));
            ia.setEvent(getTagValue("event", element));
            ia.setResponseTypes(getTagValueArray("responseType", element));
            ia.setUrgency(getTagValue("urgency", element));
            ia.setSeverity(getTagValue("severity", element));
            ia.setCertainty(getTagValue("certainty", element));
            ia.setAudience(getTagValue("audience", element));
            ia.setEffectiveDate(parseDate(getTagValue("effective", element)));
            ia.setOnsetDate(parseDate(getTagValue("onset", element)));
            ia.setExpirationDate(parseDate(getTagValue("expires", element)));
            ia.setSenderName(getTagValue("senderName", element));
            ia.setHeadline(getTagValue("headline", element));
            ia.setAlertDescription(getTagValue("description", element));
            ia.setInstruction(getTagValue("instruction", element));
            
            ia.setParameters(getValuePairJson("parameter", element));
            ia.setArea(getAreaJson("area", element));
            
            if (newObj) {
            	System.out.println("Creating alert object");
            	ia.notifyCreate();
            }
        }

    }
    
    private static Date parseDate(String dte) {
    	try {
    		return dtFormatter.parse(dte);
    	} catch(ParseException | NullPointerException e) {
    		return null;
    	}
    }  		
    		
    private static String getTagValue(String tag, Element element) {  
    	// Check if tag exists
    	if (element.getElementsByTagName(tag).getLength() > 0)
    		return element.getElementsByTagName(tag).item(0).getTextContent();
    	else 
    		return null;
    }
       
    private static List<String> getTagValueArray(String tag, Element element) {
        List<String> tag_values = new ArrayList<String>();
    	
        // Check if tag exists
        if (element.getElementsByTagName(tag).getLength() > 0) {       
	        NodeList nodeList = element.getElementsByTagName(tag);
	        for (int i = 0; i < nodeList.getLength(); i++) {
	            Node node = (Node) nodeList.item(i);
	            tag_values.add(node.getTextContent());
	        }
        }

        return tag_values;
    }
    
    
    private static String getValuePairJson(String tag, Element element) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		
    	// Check if tag exists
        if (element.getElementsByTagName(tag).getLength() > 0) {       
	        NodeList nodeList = element.getElementsByTagName(tag);
	        for (int i = 0; i < nodeList.getLength(); i++) {
	            Element childElement = (Element)  nodeList.item(i);
	        	String key = childElement.getElementsByTagName("valueName")
	        			.item(0).getTextContent();
	        	String value = childElement.getElementsByTagName("value")
	        			.item(0).getTextContent();
	        	sb.append(Json.str(key, value));
	        }
        }
        
		// remove trailing comma
		if (sb.charAt(sb.length() - 1) == ',')
			sb.setLength(sb.length() - 1);
		sb.append("}");
        return sb.toString();
    }
    
    private static String getAreaJson(String tag, Element element) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		
        // Check if tag exists
        if (element.getElementsByTagName(tag).getLength() > 0) {       
	        NodeList areaNodeList = element.getElementsByTagName(tag);
	        
	        // Loop through areas
	        for (int i = 0; i < areaNodeList.getLength(); i++) {
	            Element areaElement = (Element)  areaNodeList.item(i);
	            
	            String [] element_list = {"areaDesc","polygon",
	            		"circle","geocode","altitude","ceiling"};
	            for (String child_element: element_list)
	            	appendElementJson(child_element, areaElement, sb);
	        }
        }
		
		// remove trailing comma
		if (sb.charAt(sb.length() - 1) == ',')
			sb.setLength(sb.length() - 1);
		sb.append("}");
        return sb.toString();
    }

    
    private static void appendElementJson(String tag,
    		Element element, StringBuilder sb) {
        if (element.getElementsByTagName(tag).getLength() == 1)      
        	sb.append(Json.str(tag, element.getElementsByTagName(tag)
        			.item(0).getTextContent()));
        else if (element.getElementsByTagName(tag).getLength()  > 1)
        	sb.append(Json.sub(tag, getValuePairJson(tag, element)));
    }
    

}
	
