package com.axelor.apps.event.registration.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.event.registration.service.EventService;
import com.axelor.apps.event.registration.service.EventServiceImpl;

public class EventRegistrationModule extends AxelorModule {

	@Override
	protected void configure() {

		bind(EventService.class).to(EventServiceImpl.class);
	}
}
