package com.axelor.apps.event.registration.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.axelor.apps.event.registration.db.Address;
import com.axelor.apps.event.registration.db.Discount;
import com.axelor.apps.event.registration.db.Event;
import com.axelor.apps.event.registration.db.EventRegistration;
import com.axelor.apps.event.registration.db.repo.EventRegistrationRepository;
import com.axelor.apps.event.registration.db.repo.EventRepository;
import com.axelor.apps.event.registration.exception.IExceptionMessage;
import com.axelor.apps.event.registration.service.EventService;
import com.axelor.data.Listener;
import com.axelor.data.csv.CSVImporter;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class EventController {
	@Inject
	EventService eventService;
	@Inject
	EventRegistrationRepository eventRegistrationRepository;
	@Inject
	MetaFileRepository metaFileRepo;
	@Inject
	EventRepository eventrepo;

	public void totalEntery(ActionRequest request, ActionResponse response) {
		Event event = request.getContext().asType(Event.class);
		List<EventRegistration> eventRegistrationList = event.getEventRegistration();
		LocalDate openDate = event.getRegistrationOpen();
		LocalDate closeDate = event.getRegistrationClose();
		if (!eventRegistrationList.isEmpty()) {
			List<Long> idlist = Lists.newArrayList();
			for (EventRegistration eventRegistration : eventRegistrationList) {
				idlist.add(eventRegistration.getId());
			}
			int count = idlist.size();
			if (event.getCapacity() >= count) {
				response.setValue("totalEntery", count);
			} else {
				response.setError(I18n.get(IExceptionMessage.CAPACITY_NOT_AVAILABLE));
			}
		} else if (openDate == null && closeDate == null) {
			response.setError(I18n.get(IExceptionMessage.ENTER_DATE));
		}
		return;
	}

	public void checkCapacity(ActionRequest request, ActionResponse response) {
		Event event = request.getContext().asType(Event.class);
		if (event.getCapacity() == null) {
			response.setAlert(I18n.get(IExceptionMessage.CAPACITY_NOT_FOUND));
		} else if (event.getCapacity() < event.getTotalEntery()) {
			response.setError(I18n.get(IExceptionMessage.CHECK_CAPACITY));
		}
	}

	public void checkRegistrationCapacity(ActionRequest request, ActionResponse response) {
		EventRegistration eventRegistration = request.getContext().asType(EventRegistration.class);
		Event event = eventRegistration.getEvent();
		if (event != null) {
			String capacity = event.getCapacity().toString();
			String TotalEntery = event.getTotalEntery().toString();
			if (capacity.equals(TotalEntery)) {
				response.setError(I18n.get(IExceptionMessage.CHECK_CAPACITY));
			}
		}
	}

	public void checkDate(ActionRequest request, ActionResponse response) {
		Event event = request.getContext().asType(Event.class);
		LocalDate openDate = event.getRegistrationOpen();
		LocalDate closeDate = event.getRegistrationClose();
		LocalDateTime eventStartdate = event.getStartDate();
		if (eventStartdate != null) {
			LocalDate startDate = eventStartdate.toLocalDate();
			if (openDate != null) {
				if (startDate.isBefore(openDate)) {
					response.setError(I18n.get(IExceptionMessage.ENTER_PROPER_DATE1));
				}
			}
			if (closeDate != null) {
				if (startDate.isBefore(closeDate)) {
					response.setError(I18n.get(IExceptionMessage.ENTER_PROPER_DATE2));
				}
			}
		} else {
			response.setError(I18n.get(IExceptionMessage.ENTER_PROPER_DATE4));
		}
	}

	public void computeDiscount(ActionRequest request, ActionResponse response) {
		Discount discount = request.getContext().asType(Discount.class);
		Event event = request.getContext().getParent().asType(Event.class);
		if (event.getFees() != null) {
			discount = eventService.computeDiscountAmmount(discount, event);
			response.setValue("discountAmount", discount.getDiscountAmount());
		} else {
			response.setError(I18n.get(IExceptionMessage.ENTER_FEE));
		}
	}

	public void computeBeforDays(ActionRequest request, ActionResponse response) {
		Discount discount = request.getContext().asType(Discount.class);
		Event event = request.getContext().getParent().asType(Event.class);
		LocalDate openDate = event.getRegistrationOpen();
		LocalDate closeDate = event.getRegistrationClose();
		long noOfDaysBetween = ChronoUnit.DAYS.between(openDate, closeDate);
		System.out.println(noOfDaysBetween);
		if (discount.getBeforeDays() <= noOfDaysBetween) {
			return;
		} else {
			response.setFlash(I18n.get(IExceptionMessage.BEFORE_DAYS));
		}
	}

	public void computeAmount(ActionRequest request, ActionResponse response) {
		EventRegistration eventRegistration = request.getContext().asType(EventRegistration.class);
		Event event = eventRegistration.getEvent();
		if (event != null) {
			LocalDate openDate = event.getRegistrationOpen();
			LocalDate closeDate = event.getRegistrationClose();
			LocalDateTime registrationDate = eventRegistration.getRegistrationDate();
			LocalDate date = registrationDate.toLocalDate();
			if (openDate != null && closeDate != null) {
				if (date.isAfter(openDate) || date.equals(openDate) && date.isBefore(closeDate)
						|| date.isEqual(closeDate)) {
					List<Discount> discountList = event.getDiscount();
					if (!discountList.isEmpty()) {
						for (Discount discount : discountList) {
							eventRegistration = eventService.computeAmount(discount, eventRegistration, event);
							response.setValue("amount", eventRegistration.getAmount());
						}
					} else {
						eventRegistration.setAmount(event.getFees());
						response.setValues(eventRegistration);
					}
				} else {
					response.setError(I18n.get(IExceptionMessage.ENTER_PROPER_DATE3));
				}
			} else {
				response.setError(I18n.get(IExceptionMessage.ENTER_DATE));
			}
		} else {
			response.setAlert(I18n.get(IExceptionMessage.ENTER_EVENT));
		}
	}

	public void computeAmountCalculate(ActionRequest request, ActionResponse response) {
		Event event = request.getContext().asType(Event.class);
		event = eventService.computeAmountCollect(event);
		response.setValue("amountCollected", event.getAmountCollected());
	}

	public void computeTotalDiscount(ActionRequest request, ActionResponse response) {
		Event event = request.getContext().asType(Event.class);
		List<EventRegistration> eventRegistrationlist = event.getEventRegistration();
		BigDecimal discount = BigDecimal.ZERO;
		BigDecimal totalDiscount = BigDecimal.ZERO;
		for (EventRegistration eventRegistration : eventRegistrationlist) {
			discount = event.getFees().subtract(eventRegistration.getAmount());
			totalDiscount = discount.add(totalDiscount);
		}
		response.setValue("totalDiscount", totalDiscount);
	}

	public void importCsvData(ActionRequest request, ActionResponse response) throws IOException {

		@SuppressWarnings("unchecked")
		Map<String, Object> mapfile = (Map<String, Object>) request.getContext().get("importFile");
		if (mapfile != null) {
			Long fileId = Long.parseLong(mapfile.get("id").toString());
			MetaFile metafile = metaFileRepo.find(fileId);
			File term = MetaFiles.getPath(metafile).toFile();
			term = term.getParentFile();
			String extensionType = "text/csv";
			if (!(metafile.getFileType()).equals(extensionType)) {
				response.setError(I18n.get(IExceptionMessage.FILE_TYPE));
			} else {
				File file = new File(metafile.getFilePath());

				String name = file.getAbsoluteFile().getName().replace(file.getAbsoluteFile().getName(),
						"eventRegistration.csv");
				metafile.setFileName(name);

				File configFile = null;
				try {
					configFile = File.createTempFile("input-config", ".xml");
					InputStream bindFileInputStream = this.getClass().getResourceAsStream("/data/input-config.xml");
					FileOutputStream outputStream = new FileOutputStream(configFile);
					IOUtils.copy(bindFileInputStream, outputStream);
				} catch (Exception e) {
					e.printStackTrace();
				}
				CSVImporter importer = new CSVImporter(configFile.getAbsolutePath(), term.getAbsolutePath());

				importer.addListener(new Listener() {

					@Override
					public void imported(Model bean) {
						Event event = request.getContext().asType(Event.class);
						List<EventRegistration> eventRegistrationList = eventRegistrationRepository.all()
								.filter("self.id = ? ", event.getId()).fetch();
						Long id = event.getId();
						event = Beans.get(EventRepository.class).all().filter("self.id = ? ", id).fetchOne();
						((EventRegistration) bean).setEvent(event);

						for (EventRegistration eventRegistration : eventRegistrationList) {
							((Event) bean).addEventRegistration(eventRegistration);

						}
					}

					@Override
					public void handle(Model bean, Exception e) {

					}

					@Override
					public void imported(Integer total, Integer success) {

					}
				});
				importer.run();
			}

		} else {
			response.setAlert(I18n.get(IExceptionMessage.FILE));
		}
	}

	public void fillEventRegistrationInfo(ActionRequest request, ActionResponse response) {
		Event event = request.getContext().asType(Event.class);
		List<EventRegistration> eventRegistrationList = eventRegistrationRepository.all()
				.filter("self.id = ? ", event.getId()).fetch();
		for (EventRegistration eventRegistration : eventRegistrationList) {
			if (event.getReference() != null) {
				if (event.getReference().equals(eventRegistration.getEvent().getReference())) {
					event.addEventRegistration(eventRegistration);
					response.setValues(event);
				}
			}
		}
	}

}
