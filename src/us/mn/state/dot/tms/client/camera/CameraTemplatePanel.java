/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020 SRF Consulting Group
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
package us.mn.state.dot.tms.client.camera;

import java.util.ArrayList;

import us.mn.state.dot.tms.CameraTemplate;
import us.mn.state.dot.tms.CameraVidSourceOrder;
import us.mn.state.dot.tms.CameraVidSourceOrderHelper;
import us.mn.state.dot.tms.client.proxy.ProxyTableModel;
import us.mn.state.dot.tms.client.proxy.ProxyTablePanel;

/**
 * A panel for displaying and editing camera templates. Overrides the
 * deleteSelectedProxy method inherited from ProxyTablePanel to delete
 * CameraVidSourceOrder objects before deleting camera templates.
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class CameraTemplatePanel extends ProxyTablePanel<CameraTemplate> {
	public CameraTemplatePanel(ProxyTableModel<CameraTemplate> m) {
		super(m);
	}
	
	/** Delete the selected CameraTemplate. Also deletes any
	 *  CameraVidSourceOrder objects that depend on it.
	 */
	@Override
	protected void deleteSelectedProxy() {
		CameraTemplate ct = getSelectedProxy();
		
		if (ct != null) {
			// find all CameraVidSourceOrder objects that depend on ct
			ArrayList<CameraVidSourceOrder> cvos =
					CameraVidSourceOrderHelper.listForCameraTemplate(
							ct.getName());
			
			// delete them
			for (CameraVidSourceOrder cvo: cvos)
				cvo.destroy();
			
			// then delete the camera template
			ct.destroy();
		}
	}
}
