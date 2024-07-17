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
use crate::card::{inactive_attr, Card, View};
use crate::cio::{ControllerIo, ControllerIoAnc};
use crate::util::{ContainsLower, Fields, HtmlStr, Input};
use resources::Res;
use serde::{Deserialize, Serialize};
use std::borrow::Cow;

/// Video Monitor
#[derive(Debug, Default, Deserialize, Serialize, PartialEq)]
pub struct VideoMonitor {
    pub name: String,
    pub mon_num: u32,
    pub controller: Option<String>,
    // secondary attributes
    pub pin: Option<u32>,
}

type VideoMonitorAnc = ControllerIoAnc<VideoMonitor>;

impl VideoMonitor {
    /// Convert to Compact HTML
    fn to_html_compact(&self, anc: &VideoMonitorAnc) -> String {
        let name = HtmlStr::new(self.name());
        let item_state = anc.item_state(self);
        let inactive = inactive_attr(self.controller.is_some());
        let mon_num = self.mon_num;
        format!(
            "<div class='title row'>{name} {item_state}</div>\
            <div class='info fill{inactive}'>{mon_num}</div>"
        )
    }

    /// Convert to Status HTML
    fn to_html_status(&self) -> String {
        let title = self.title(View::Status);
        let mon_num = self.mon_num;
        format!(
            "{title}\
            <div class='row'>\
              <span class='info'>{mon_num}</span>\
            </div>"
        )
    }

    /// Convert to Setup HTML
    fn to_html_setup(&self, anc: &VideoMonitorAnc) -> String {
        let title = self.title(View::Setup);
        let controller = anc.controller_html(self);
        let pin = anc.pin_html(self.pin);
        let footer = self.footer(true);
        format!("{title}{controller}{pin}{footer}")
    }
}

impl ControllerIo for VideoMonitor {
    /// Get controller
    fn controller(&self) -> Option<&str> {
        self.controller.as_deref()
    }
}

impl Card for VideoMonitor {
    type Ancillary = VideoMonitorAnc;

    /// Display name
    const DNAME: &'static str = "📺 Video Monitor";

    /// Get the resource
    fn res() -> Res {
        Res::VideoMonitor
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
    fn is_match(&self, search: &str, anc: &VideoMonitorAnc) -> bool {
        self.name.contains_lower(search)
            || self.mon_num.to_string().contains(search)
            || anc.item_state(self).is_match(search)
    }

    /// Convert to HTML view
    fn to_html(&self, view: View, anc: &VideoMonitorAnc) -> String {
        match view {
            View::Create => self.to_html_create(anc),
            View::Status => self.to_html_status(),
            View::Setup => self.to_html_setup(anc),
            _ => self.to_html_compact(anc),
        }
    }

    /// Get changed fields from Setup form
    fn changed_fields(&self) -> String {
        let mut fields = Fields::new();
        fields.changed_input("controller", &self.controller);
        fields.changed_input("pin", self.pin);
        fields.into_value().to_string()
    }
}
