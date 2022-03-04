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
use crate::util::Dom;
use crate::Result;
use serde::de::DeserializeOwned;
use serde_json::map::Map;
use serde_json::Value;
use std::fmt;
use wasm_bindgen::JsValue;
use web_sys::Document;

/// CSS class for titles
const TITLE: &str = "title";

/// CSS class for names
pub const NAME: &str = "ob_name";

/// Compact "Create" card
const CREATE_COMPACT: &str = "<span class='create'>Create 🆕</span>";

/// Type of card
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum CardType {
    /// Compact in list
    Compact,

    /// Create card
    Create,

    /// Status card
    Status,

    /// Edit card
    Edit,
}

/// A card can be displayed in a card list
pub trait Card: fmt::Display + DeserializeOwned {
    const TNAME: &'static str;
    const ENAME: &'static str;
    const UNAME: &'static str;
    const HAS_LOCATION: bool = false;
    const HAS_STATUS: bool = false;

    /// Create from a JSON value
    fn new(json: &JsValue) -> Result<Self> {
        json.into_serde::<Self>().map_err(|e| e.to_string().into())
    }

    /// Build form using JSON value
    fn build_card(
        name: &str,
        json: &Option<JsValue>,
        ct: CardType,
    ) -> Result<String> {
        match json {
            Some(json) => {
                let val = Self::new(json)?;
                match ct {
                    CardType::Compact => Ok(val.to_html_compact()),
                    CardType::Status if Self::HAS_STATUS => {
                        Ok(val.status_card())
                    }
                    _ => Ok(val.edit_card()),
                }
            }
            None => match ct {
                CardType::Create => Ok(Self::create_card(name)),
                _ => Ok(CREATE_COMPACT.into()),
            },
        }
    }

    /// Build a create card
    fn create_card(name: &str) -> String {
        let ename = Self::ENAME;
        let create = Self::html_create(name);
        format!(
            "<div class='row'>\
              <div class='{TITLE}'>{ename}</div>\
              <span class='{NAME}'>🆕</span>\
            </div>\
            {create}
            <div class='row'>\
              <button id='ob_close' type='button'>❌ Close</button>\
              <button id='ob_save' type='button'>🖍️ Save</button>\
            </div>"
        )
    }

    /// Get row for create card
    fn html_create(name: &str) -> String {
        format!(
            "<div class='row'>\
              <label for='create_name'>Name</label>\
              <input id='create_name' maxlength='24' size='24' value='{name}'/>\
            </div>"
        )
    }

    /// Get value to create a new object
    fn create_value(doc: &Document) -> Result<String> {
        if let Some(name) = doc.input_parse::<String>("create_name") {
            if !name.is_empty() {
                let mut obj = Map::new();
                obj.insert("name".to_string(), Value::String(name));
                return Ok(Value::Object(obj).to_string());
            }
        }
        Err("name missing".into())
    }

    /// Build a status card
    fn status_card(&self) -> String {
        let ename = Self::ENAME;
        let status = self.to_html_status();
        let location = if Self::HAS_LOCATION {
            // could use 🌐 instead
            "<button id='ob_loc' type='button'>🗺️ Location</button>"
        } else {
            ""
        };
        format!(
            "<div class='row'>\
              <div class='{TITLE}'>{ename}</div>\
              <span class='{NAME}'>{self}</span>\
            </div>\
            {status}\
            <div class='row'>\
              <button id='ob_close' type='button'>❌ Close</button>\
              {location}
              <button id='ob_edit' type='button'>📝 Edit</button>\
            </div>"
        )
    }

    /// Convert to status HTML
    fn to_html_status(&self) -> String {
        unreachable!()
    }

    /// Build an edit card
    fn edit_card(&self) -> String {
        let ename = Self::ENAME;
        let edit = self.to_html_edit();
        format!(
            "<div class='row'>\
              <div class='{TITLE}'>{ename}</div>\
              <span class='{NAME}'>{self}</span>\
            </div>\
            {edit}\
            <div class='row'>\
              <button id='ob_close' type='button'>❌ Close</button>\
              <button id='ob_delete' type='button'>🗑️ Delete</button>\
              <button id='ob_save' type='button'>🖍️ Save</button>\
            </div>"
        )
    }

    /// Convert to edit HTML
    fn to_html_edit(&self) -> String {
        unreachable!()
    }

    /// Get changed fields from Edit form
    fn changed_fields(doc: &Document, json: &JsValue) -> Result<String>;

    /// Check if a search string matches
    fn is_match(&self, _tx: &str) -> bool {
        false
    }

    /// Get next suggested name
    fn next_name(_obs: &[Self]) -> String {
        "".into()
    }

    /// Build a list of cards from a JSON array
    fn build_cards(json: &JsValue, tx: &str) -> Result<String> {
        let tname = Self::TNAME;
        let mut html = String::new();
        html.push_str("<ul class='cards'>");
        let obs = json
            .into_serde::<Vec<Self>>()
            .map_err(|e| JsValue::from(e.to_string()))?;
        let next_name = Self::next_name(&obs);
        if tx.is_empty() {
            // the "Create" card has id "{tname}_" and next available name
            html.push_str(&format!(
                "<li id='{tname}_' name='{next_name}' class='card'>\
                    {CREATE_COMPACT}\
                </li>"
            ));
        }
        // TODO: split this into async calls so it can be cancelled
        for ob in obs.iter().filter(|ob| ob.is_match(tx)) {
            html.push_str(&format!(
                "<li id='{tname}_{ob}' name='{ob}' class='card'>"
            ));
            html.push_str(&ob.to_html_compact());
            html.push_str("</li>");
        }
        html.push_str("</ul>");
        Ok(html)
    }

    /// Convert to compact HTML
    fn to_html_compact(&self) -> String;
}

/// Get attribute for disabled cards
pub fn disabled_attr(enabled: bool) -> &'static str {
    if enabled {
        ""
    } else {
        " class='disabled'"
    }
}
