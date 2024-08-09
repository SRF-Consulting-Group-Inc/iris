// Copyright (C) 2022-2024  Minnesota Department of Transportation
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
use crate::card::{Card, View};
use crate::cio::{ControllerIo, ControllerIoAnc};
use crate::util::{ContainsLower, Fields, HtmlStr, Input, TextArea};
use resources::Res;
use serde::{Deserialize, Serialize};
use std::borrow::Cow;

/// GPS
#[derive(Debug, Default, Deserialize, Serialize, PartialEq)]
pub struct Gps {
    pub name: String,
    pub controller: Option<String>,
    pub notes: Option<String>,
    // secondary attributes
    pub pin: Option<u32>,
    pub geo_loc: Option<String>,
}

type GpsAnc = ControllerIoAnc<Gps>;

impl Gps {
    /// Convert to Compact HTML
    fn to_html_compact(&self, anc: &GpsAnc) -> String {
        let name = HtmlStr::new(self.name());
        let item_states = anc.item_states(self);
        format!("<div class='end'>{name} {item_states}</div>")
    }

    /// Convert to Setup HTML
    fn to_html_setup(&self, anc: &GpsAnc) -> String {
        let title = self.title(View::Setup);
        let notes = HtmlStr::new(&self.notes);
        let controller = anc.controller_html(self);
        let pin = anc.pin_html(self.pin);
        let geo_loc = HtmlStr::new(&self.geo_loc);
        let footer = self.footer(true);
        format!(
            "{title}\
            <div class='row'>\
              <label for='notes'>Notes</label>\
              <textarea id='notes' maxlength='255' rows='4' \
                        cols='24'>{notes}</textarea>\
            </div>\
            <div class='row'>\
              <label for='geo_loc'>Device Loc</label>\
              <input id='geo_loc' maxlength='20' size='20' \
                     value='{geo_loc}'>\
            </div>\
            {controller}\
            {pin}\
            {footer}"
        )
    }
}

impl ControllerIo for Gps {
    /// Get controller name
    fn controller(&self) -> Option<&str> {
        self.controller.as_deref()
    }
}

impl Card for Gps {
    type Ancillary = GpsAnc;

    /// Display name
    const DNAME: &'static str = "🌐 Gps";

    /// Get the resource
    fn res() -> Res {
        Res::Gps
    }

    /// Get the name
    fn name(&self) -> Cow<str> {
        Cow::Borrowed(&self.name)
    }

    /// Set the name
    fn with_name(mut self, name: &str) -> Self {
        self.name = name.to_string();
        self
    }

    /// Check if a search string matches
    fn is_match(&self, search: &str, _anc: &GpsAnc) -> bool {
        self.name.contains_lower(search) || self.notes.contains_lower(search)
    }

    /// Convert to HTML view
    fn to_html(&self, view: View, anc: &GpsAnc) -> String {
        match view {
            View::Create => self.to_html_create(anc),
            View::Setup => self.to_html_setup(anc),
            _ => self.to_html_compact(anc),
        }
    }

    /// Get changed fields from Setup form
    fn changed_setup(&self) -> String {
        let mut fields = Fields::new();
        fields.changed_text_area("notes", &self.notes);
        fields.changed_input("geo_loc", &self.geo_loc);
        fields.changed_input("controller", &self.controller);
        fields.changed_input("pin", self.pin);
        fields.into_value().to_string()
    }
}
