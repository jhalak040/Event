package com.axelor.app.event.db.repo;

import java.util.Map;

import com.axelor.apps.event.registration.db.Event;
import com.axelor.apps.event.registration.db.repo.EventRepository;

public class EventRepo extends EventRepository {

	public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
		if (!context.containsKey("json-enhance")) {
			return json;
		}
		try {
			Long id = (Long) json.get("id");
			Event event = find(id);

			json.put("venu", event.getVenu().getFullName());
		} catch (Exception e) {
		}

		return json;
	}

}
