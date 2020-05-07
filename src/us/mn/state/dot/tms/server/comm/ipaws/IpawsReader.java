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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import us.mn.state.dot.tms.IncidentHelper;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.server.IpawsAlertImpl;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.Json;

/**

 *
 * @author Michael Janson
 */

public class IpawsReader {
	
	private static final String dtFormat = "YYYY-MM-dd'T'HH:mm:ssX";
	private static final SimpleDateFormat dtFormatter = new SimpleDateFormat(dtFormat);
	
	
//	/** Cache of incident proxy objects */
//	private final TypeCache<IpawsAlert> cache;

    public static void readIpaws() throws IOException {
        File folder = new File("\\\\srfgroup.loc\\Shares\\RDF\\mjanson\\Folders\\Documents\\ipaws_files");
        //File folder = new File("\\\\srfgroup.loc\\Shares\\RDF\\mjanson\\Folders\\Documents\\srf-iris-ipaws workspace\\srf-iris-ipaws\\var\\log\\iris\\test");

        File[] files = folder.listFiles();
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
        	dBuilder = dbFactory.newDocumentBuilder();
		    for (File xmlFile : files) {
		        	InputStream in = new GZIPInputStream(new FileInputStream(xmlFile));
		            Document doc = dBuilder.parse(in);
		            doc.getDocumentElement().normalize();
		
		            NodeList nodeList = doc.getElementsByTagName("alert");
		            try {
			            for (int i = 0; i < nodeList.getLength(); i++) {
			                getIpawsAlert(nodeList.item(i));
			            }
		            }catch(SonarException e) {
//		        		if (e.getMessage().contains("Name already exists")) continue;
//		        		else throw new SonarException(e);
		            }
		    }
        }catch(ParserConfigurationException | SAXException | ParseException | TMSException e) {
        	e.printStackTrace();
        }
    }
    
    public static void readIpaws(InputStream is) throws IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
        	dBuilder = dbFactory.newDocumentBuilder();
	            Document doc = dBuilder.parse(is);
	            doc.getDocumentElement().normalize();
	
	            NodeList nodeList = doc.getElementsByTagName("alert");
		            for (int i = 0; i < nodeList.getLength(); i++) {
		                getIpawsAlert(nodeList.item(i));
		            }
        }catch(ParserConfigurationException | SAXException | ParseException | TMSException | SonarException e) {
        	e.printStackTrace();
        }
    }
    
    private static void getIpawsAlert(Node node) throws ParseException, SonarException, TMSException {

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;

            IpawsAlertImpl ia = new IpawsAlertImpl(getTagValue("identifier", element));
            ia.setIdentifier(getTagValue("identifier", element));
            ia.setSender(getTagValue("sender", element));
            ia.setSentDate(checkDate(getTagValue("sent", element)));
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
            ia.setEffectiveDate(checkDate(getTagValue("effective", element)));
            ia.setOnsetDate(checkDate(getTagValue("onset", element)));
            ia.setExpirationDate(checkDate(getTagValue("expires", element)));
            ia.setSenderName(getTagValue("senderName", element));
            ia.setHeadline(getTagValue("headline", element));
            ia.setAlertDescription(getTagValue("description", element));
            ia.setInstruction(getTagValue("instruction", element));
            
            ia.setParameters(getValuePairJson("parameter", element));
            ia.setArea(getAreaJson("area", element));
            
            ia.notifyCreate();
        }

    }
    
    private static Date checkDate(String dte) {
    	try{
    		return dtFormatter.parse(dte);
    	}catch(ParseException | NullPointerException e) {
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
	        	String key = childElement.getElementsByTagName("valueName").item(0).getTextContent();
	        	String value = childElement.getElementsByTagName("value").item(0).getTextContent();
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
	            
	            String [] element_list = {"areaDesc","polygon","circle","geocode","altitude","ceiling"};
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

    
    private static void appendElementJson(String tag, Element element, StringBuilder sb) {
        if (element.getElementsByTagName(tag).getLength() == 1)      
        	sb.append(Json.str(tag, element.getElementsByTagName(tag).item(0).getTextContent()));
        else if (element.getElementsByTagName(tag).getLength()  > 1)
        	sb.append(Json.sub(tag, getValuePairJson(tag, element)));
    }
    

}
	
