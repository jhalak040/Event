package com.axelor.apps.event.registration.service;

import com.axelor.apps.event.registration.db.Discount;
import com.axelor.apps.event.registration.db.Event;
import com.axelor.apps.event.registration.db.EventRegistration;

public interface EventService {

	public Discount computeDiscountAmmount(Discount discount, Event event);

	public EventRegistration computeAmount(Discount discount, EventRegistration eventRegistration, Event event);

	public Event computeAmountCollect(Event event);

	public Event fillEventRegistrationInfo(Event event, EventRegistration eventRegistration);

}
