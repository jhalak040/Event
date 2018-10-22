package com.axelor.apps.event.registration.service;

import com.axelor.apps.event.registration.db.EventRegistration;

import com.axelor.meta.db.MetaFile;

public interface EventImportService {

	public EventRegistration importData(String typeSelect, MetaFile dataFile);
}
