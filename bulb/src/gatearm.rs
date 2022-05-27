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
use crate::controller::Controller;
use crate::error::Result;
use crate::resource::{
    disabled_attr, AncillaryData, Card, View, EDIT_BUTTON, NAME,
};
use crate::util::{ContainsLower, Fields, HtmlStr, Input, OptVal};
use serde::{Deserialize, Serialize};
use std::borrow::{Borrow, Cow};
use std::fmt;
use wasm_bindgen::JsValue;

/// Gate arm states
#[derive(Debug, Deserialize, Serialize)]
pub struct GateArmState {
    pub id: u32,
    pub description: String,
}

/// Gate Arm
#[derive(Debug, Default, Deserialize, Serialize)]
pub struct GateArm {
    pub name: String,
    pub location: Option<String>,
    pub controller: Option<String>,
    pub notes: String,
    pub arm_state: u32,
    // full attributes
    pub pin: Option<u32>,
}

/// Ancillary gate arm data
#[derive(Debug, Default)]
pub struct GateArmAnc {
    pub controller: Option<Controller>,
    pub states: Option<Vec<GateArmState>>,
}

impl GateArmAnc {
    fn controller_button(&self) -> String {
        match &self.controller {
            Some(ctrl) => ctrl.button_html(),
            None => "<span></span>".into(),
        }
    }

    /// Get state description
    fn state(&self, pri: &GateArm) -> &str {
        if let Some(states) = &self.states {
            for state in states {
                if pri.arm_state == state.id {
                    return &state.description;
                }
            }
        }
        ""
    }
}

const GATE_ARM_STATE_URI: &str = "/iris/gate_arm_state";

impl AncillaryData for GateArmAnc {
    type Primary = GateArm;

    /// Get ancillary URI
    fn uri(&self, view: View, pri: &GateArm) -> Option<Cow<str>> {
        match (view, &self.states, &self.controller, &pri.controller()) {
            (_, None, _, _) => Some(GATE_ARM_STATE_URI.into()),
            (View::Status(_), _, None, Some(ctrl)) => {
                Some(format!("/iris/api/controller/{}", &ctrl).into())
            }
            _ => None,
        }
    }

    /// Put ancillary JSON data
    fn set_json(
        &mut self,
        view: View,
        pri: &GateArm,
        json: JsValue,
    ) -> Result<()> {
        if let Some(uri) = self.uri(view, pri) {
            match uri.borrow() {
                GATE_ARM_STATE_URI => {
                    self.states = Some(json.into_serde::<Vec<GateArmState>>()?);
                }
                _ => self.controller = Some(json.into_serde::<Controller>()?),
            }
        }
        Ok(())
    }
}

impl GateArm {
    pub const RESOURCE_N: &'static str = "gate_arm";

    /// Get controller
    fn controller(&self) -> Option<&str> {
        self.controller.as_deref()
    }

    /// Convert to Compact HTML
    fn to_html_compact(&self, anc: &GateArmAnc) -> String {
        let arm_state = anc.state(self);
        let disabled = disabled_attr(self.controller.is_some());
        let location = HtmlStr::new(&self.location);
        format!(
            "<div class='{NAME} right'>{arm_state} {self}</div>\
            <div class='info left{disabled}'>{location}</div>"
        )
    }

    /// Convert to Status HTML
    fn to_html_status(&self, anc: &GateArmAnc) -> String {
        let ctrl_button = anc.controller_button();
        format!(
            "<div class='row'>\
              {ctrl_button}\
              {EDIT_BUTTON}\
            </div>"
        )
    }

    /// Convert to Edit HTML
    fn to_html_edit(&self) -> String {
        let controller = HtmlStr::new(&self.controller);
        let pin = OptVal(self.pin);
        format!(
            "<div class='row'>\
               <label for='controller'>Controller</label>\
               <input id='controller' maxlength='20' size='20' \
                      value='{controller}'/>\
             </div>\
             <div class='row'>\
               <label for='pin'>Pin</label>\
               <input id='pin' type='number' min='1' max='104' \
                      size='8' value='{pin}'/>\
             </div>"
        )
    }
}

impl fmt::Display for GateArm {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", HtmlStr::new(&self.name))
    }
}

impl Card for GateArm {
    type Ancillary = GateArmAnc;

    /// Set the name
    fn with_name(mut self, name: &str) -> Self {
        self.name = name.to_string();
        self
    }

    /// Check if a search string matches
    fn is_match(&self, search: &str, anc: &GateArmAnc) -> bool {
        self.name.contains_lower(search) || anc.state(self).contains(search)
    }

    /// Convert to HTML view
    fn to_html(&self, view: View, anc: &GateArmAnc) -> String {
        match view {
            View::Create => self.to_html_create(anc),
            View::Compact => self.to_html_compact(anc),
            View::Status(_) => self.to_html_status(anc),
            View::Edit => self.to_html_edit(),
            _ => unreachable!(),
        }
    }

    /// Get changed fields from Edit form
    fn changed_fields(&self) -> String {
        let mut fields = Fields::new();
        fields.changed_input("controller", &self.controller);
        fields.changed_input("pin", self.pin);
        fields.into_value().to_string()
    }
}
