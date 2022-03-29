// Copyright (C) 2022  Minnesota Department of Transportation
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
use crate::device::{Device, DeviceAnc};
use crate::error::Result;
use crate::resource::{disabled_attr, Card, NAME};
use crate::util::{ContainsLower, Dom, HtmlStr, OptVal};
use serde::{Deserialize, Serialize};
use serde_json::map::Map;
use serde_json::Value;
use std::fmt;
use wasm_bindgen::JsValue;
use web_sys::Document;

/// Ramp Meter
#[derive(Debug, Default, Deserialize, Serialize)]
pub struct RampMeter {
    pub name: String,
    pub location: Option<String>,
    pub controller: Option<String>,
    // full attributes
    pub geo_loc: Option<String>,
    pub pin: Option<u32>,
}

type RampMeterAnc = DeviceAnc<RampMeter>;

impl RampMeter {
    pub const RESOURCE_N: &'static str = "ramp_meter";
}

impl fmt::Display for RampMeter {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", HtmlStr::new(&self.name))
    }
}

impl Device for RampMeter {
    /// Get controller
    fn controller(&self) -> Option<&str> {
        self.controller.as_deref()
    }
}

impl Card for RampMeter {
    type Ancillary = RampMeterAnc;

    /// Set the name
    fn with_name(mut self, name: &str) -> Self {
        self.name = name.to_string();
        self
    }

    /// Get geo location name
    fn geo_loc(&self) -> Option<&str> {
        self.geo_loc.as_deref()
    }

    /// Check if a search string matches
    fn is_match(&self, search: &str, _anc: &RampMeterAnc) -> bool {
        self.name.contains_lower(search) || self.location.contains_lower(search)
    }

    /// Convert to compact HTML
    fn to_html_compact(&self, _anc: &RampMeterAnc) -> String {
        let location = HtmlStr::new(&self.location).with_len(12);
        let disabled = disabled_attr(self.controller.is_some());
        format!(
            "<span{disabled}>{location}</span>\
            <span class='{NAME}'>{self}</span>"
        )
    }

    /// Convert to status HTML
    fn to_html_status(&self, _anc: &RampMeterAnc) -> String {
        let location = HtmlStr::new(&self.location).with_len(64);
        format!(
            "<div class='row'>\
              <span class='info'>{location}</span>\
            </div>"
        )
    }

    /// Convert to edit HTML
    fn to_html_edit(&self, anc: &RampMeterAnc) -> String {
        let ctrl_loc = anc.controller_loc_html();
        let controller = HtmlStr::new(&self.controller);
        let pin = OptVal(self.pin);
        format!(
            "{ctrl_loc}\
             <div class='row'>\
               <label for='edit_ctrl'>Controller</label>\
               <input id='edit_ctrl' maxlength='20' size='20' \
                      value='{controller}'/>\
             </div>\
             <div class='row'>\
               <label for='edit_pin'>Pin</label>\
               <input id='edit_pin' type='number' min='1' max='104' \
                      size='8' value='{pin}'/>\
             </div>"
        )
    }

    /// Get changed fields from Edit form
    fn changed_fields(doc: &Document, json: &JsValue) -> Result<String> {
        let val = Self::new(json)?;
        let mut obj = Map::new();
        let ctrl = doc
            .input_parse::<String>("edit_ctrl")
            .filter(|c| !c.is_empty());
        if ctrl != val.controller {
            obj.insert("controller".to_string(), OptVal(ctrl).into());
        }
        let pin = doc.input_parse::<u32>("edit_pin");
        if pin != val.pin {
            obj.insert("pin".to_string(), OptVal(pin).into());
        }
        Ok(Value::Object(obj).to_string())
    }
}
