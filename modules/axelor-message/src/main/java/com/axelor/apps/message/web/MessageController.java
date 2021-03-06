/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.message.web;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.event.registration.db.Event;
import com.axelor.apps.event.registration.db.EventRegistration;
import com.axelor.apps.event.registration.db.repo.EventRepository;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.repo.MessageRepository;
import com.axelor.apps.message.exception.IExceptionMessage;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.tool.ModelTool;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MessageController {

	private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Inject
	private MessageRepository messageRepo;

	@Inject
	private MessageService messageService;
	@Inject
	EventRepository eventRepo;

	public void sendMessage(ActionRequest request, ActionResponse response) {
		Message message = request.getContext().asType(Message.class);

		try {
			message = messageService.sendMessage(messageRepo.find(message.getId()));
			response.setReload(true);

			if (message.getStatusSelect() == MessageRepository.STATUS_SENT) {

				if (message.getSentByEmail()) {
					response.setFlash(I18n.get(IExceptionMessage.MESSAGE_4));

					Event event = eventRepo.all().fetchOne();
					List<EventRegistration> emalList = event.getEventRegistration();
					for (EventRegistration eventRegistration : emalList) {
						/*if (eventRegistration.getEmail() != null) {*/
							eventRegistration.setEmailSent(true);
							response.setValue("emailSent", true);
						
					}
				} else {
					response.setFlash(I18n.get(IExceptionMessage.MESSAGE_5));
				}

			} else {
				response.setFlash(I18n.get(IExceptionMessage.MESSAGE_6));
			}
		} catch (AxelorException e) {
			TraceBackService.trace(response, e);
		}
	}

	@SuppressWarnings("unchecked")
	public void sendMessages(ActionRequest request, ActionResponse response) {
		List<Integer> idList = (List<Integer>) request.getContext().get("_ids");
		try {
			if (idList == null) {
				throw new AxelorException(TraceBackRepository.CATEGORY_MISSING_FIELD,
						I18n.get(IExceptionMessage.MESSAGE_MISSING_SELECTED_MESSAGES));
			}
			int error = ModelTool.apply(Message.class, idList, model -> messageService.sendMessage((Message) model));
			response.setFlash(String.format(I18n.get(IExceptionMessage.MESSAGES_SENT), idList.size() - error, error));
			response.setReload(true);
		} catch (AxelorException e) {
			TraceBackService.trace(response, e);
		}
	}

	@SuppressWarnings("unchecked")
	public void regenerateMessages(ActionRequest request, ActionResponse response) {
		List<Integer> idList = (List<Integer>) request.getContext().get("_ids");
		try {
			if (idList == null) {
				throw new AxelorException(TraceBackRepository.CATEGORY_MISSING_FIELD,
						I18n.get(IExceptionMessage.MESSAGE_MISSING_SELECTED_MESSAGES));
			}
			int error = ModelTool.apply(Message.class, idList,
					model -> messageService.regenerateMessage((Message) model));
			response.setFlash(
					String.format(I18n.get(IExceptionMessage.MESSAGES_REGENERATED), idList.size() - error, error));
			response.setReload(true);
		} catch (AxelorException e) {
			TraceBackService.trace(response, e);
		}
	}

	public void getEmails(ActionRequest request, ActionResponse response) {
		Message message = request.getContext().asType(Message.class);
		long objectId = message.getTemplate().getMetaModel().getId();
		Event event = eventRepo.all().filter("self.id=?", objectId).fetchOne();
		List<EventRegistration> emalList = event.getEventRegistration();
		for (EventRegistration eventRegistration : emalList) {
			if (eventRegistration.getEmail() != null) {
				eventRegistration.setEmailSent(true);
				response.setValue("emailSent", true);
			}
		}
	}

}
