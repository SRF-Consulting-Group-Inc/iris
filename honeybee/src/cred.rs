// cred.rs
//
// Copyright (C) 2021-2024  Minnesota Department of Transportation
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
use crate::error::{Error, Result};
use crate::sonar::Messenger;
use serde::{Deserialize, Serialize};
use tower_sessions::Session;

/// Switch to bypass sonar connection (debug mode)
const BYPASS_SONAR: bool = false;

/// IRIS host name
const HOST: &str = "localhost.localdomain";

/// Session key for credentials
const CRED_KEY: &str = "cred";

/// Authentication credentials
#[derive(Clone, Debug, Eq, PartialEq, Deserialize, Serialize)]
pub struct Credentials {
    /// Sonar username
    username: String,
    /// Sonar password
    password: String,
}

impl Credentials {
    /// Load credentials from session
    pub async fn load(session: &Session) -> Result<Self> {
        session.get(CRED_KEY).await?.ok_or(Error::Unauthorized)
    }

    /// Store credentials in session
    pub async fn store(&self, session: &Session) -> Result<()> {
        Ok(session.insert(CRED_KEY, self).await?)
    }

    /// Get user name as string slice
    pub fn user(&self) -> &str {
        &self.username
    }

    /// Authenticate with IRIS server
    pub async fn authenticate(&self) -> Result<Option<Messenger>> {
        if BYPASS_SONAR {
            Ok(None)
        } else {
            let mut msn = Messenger::new(HOST, 1037).await?;
            msn.login(&self.username, &self.password).await?;
            Ok(Some(msn))
        }
    }
}