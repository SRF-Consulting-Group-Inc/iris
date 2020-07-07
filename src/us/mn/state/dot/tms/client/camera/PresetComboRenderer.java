/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014-2016  Minnesota Department of Transportation
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

import us.mn.state.dot.tms.CameraPreset;
import us.mn.state.dot.tms.Direction;
import us.mn.state.dot.tms.client.widget.IListCellRenderer;

/**
 * Renderer for camera preset combo boxes.
 *
 * @author Douglas Lau
 */
public class PresetComboRenderer extends IListCellRenderer<CameraPreset> {

	/** Convert camera preset to a string */
	@Override
	protected String valueToString(CameraPreset cp) {
		Direction dir = Direction.fromOrdinal(cp.getDirection());
		if (dir != Direction.UNKNOWN)
			return cp.getCamera().getName() + ":" + dir.det_dir;
		else
			return cp.getCamera().getName() + ":" + cp.getPresetNum();
	}
}
