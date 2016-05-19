package de.benjaminborbe.bot.highrise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.algaworks.highrisehq.Highrise;
import com.algaworks.highrisehq.HighriseException;
import com.algaworks.highrisehq.bean.Person;
import com.algaworks.highrisehq.bean.PhoneNumber;
import com.fasterxml.jackson.core.JsonProcessingException;

import de.benjaminborbe.bot.agent.MessageHandler;
import de.benjaminborbe.bot.agent.Request;
import de.benjaminborbe.bot.agent.Response;
import de.benjaminborbe.bot.highrise.messagehandler.HelpMessage;

public class HighriseHandler implements MessageHandler {

  private static final Logger logger = LoggerFactory.getLogger(HighriseHandler.class);

  private HashMap<String, ConversionState> userStates = new HashMap<>();

  private List<ConversionState> conversionStates = new LinkedList<>();

  private List<HelpMessage> messageHandlers = new ArrayList<>();

  private UserDataService userDataService;

  @Inject
  public HighriseHandler(UserDataService userDataService) {
    this.userDataService = userDataService;
    conversionStates.add(0, new ConversionStateSubdomain());

    messageHandlers.add(new HelpMessage());

  }

  @Override
  public Collection<Response> HandleMessage(final Request request) {

    final Response response = new Response();

    String message = request.getMessage();

    for (HelpMessage messageHandler : messageHandlers) {
      if (messageHandler.matches(message)) {
        response.setMessage(messageHandler.handleMessage(message));
        return Collections.singletonList(response);
      }
    }

    if (message.startsWith("/highrise subdomain")) {
      String user = message.substring(new String("/highrise subdomain ").length());

      try {
        userDataService.storeUserName(request.getAuthToken(), user);
        response.setMessage("Alright, Your Highrise Subdomain is now set to: " + user);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        response.setMessage(
            "Sorry, but the storing process of your subdomain failed, unfortunately. I am not smart enough to know why, yet. " + user);
      }

    } else if (message.startsWith("/highrise apitoken")) {
      String pass = message.substring(new String("/highrise apitoken ").length());
      try {
        userDataService.storeToken(request.getAuthToken(), pass);
        response.setMessage("Noted. Your API token for Highrise is now set to: " + pass);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        response.setMessage(
            "Ouch! Something went terribly wrong. Storing of your API token failed. Unfortunately I have no glue, why this is.");
      }

    } else if (message.startsWith("/highrise search ")) {
      searchForPeople(request, response);

    } else {
      return Collections.emptyList();
    }

    return Collections.singletonList(response);
  }

  private void searchForPeople(final Request request, final Response response) {
    String searchString = request.getMessage().substring(new String("/highrise search ").length());
    try {
      Credentials credentials = userDataService.getCredentials(request.getAuthToken());
      Highrise highrise = new Highrise(credentials.getUserName(), credentials.getApiKey());
      List<Person> persons = highrise.getPeopleManager().searchByCustomField("term", searchString);
      if (persons.size() == 0) {
        response.setMessage("sorry, i found no results for " + searchString);
      } else {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("I found " + persons.size() + " contact(s) for you:");
        boolean extended = persons.size() > 3 ? false : true;
        for (Person person : persons) {

          if (extended) {
            stringBuilder.append(person.getFirstName() + " " + person.getLastName());
            if (person.getContactData().getEmailAddresses().size() > 0) {
              stringBuilder.append("\nE-Mail:");
              stringBuilder.append(person.getContactData().getEmailAddresses().get(0).getAddress());
            }
            if (person.getContactData().getPhoneNumbers().size() > 0) {
              for (PhoneNumber phoneNumber : person.getContactData().getPhoneNumbers()) {
                stringBuilder.append("\nPhone: " + phoneNumber.getNumber());
              }

            }
            if (!person.getCompanyName().isEmpty()) {
              stringBuilder.append("\nCompany: " + person.getCompanyName());
            }

            stringBuilder.append("\n------------------------------\n");
          } else {

            stringBuilder.append(person.getFirstName() + " " + person.getLastName());
            if (person.getContactData().getEmailAddresses().size() > 0) {
              stringBuilder.append("\n");
              stringBuilder.append(person.getContactData().getEmailAddresses().get(0).getAddress());
            }
            stringBuilder.append("\n------------------------------\n");
          }

          stringBuilder.append("\n");
          response.setMessage(stringBuilder.toString());
        }
      }
    } catch (HighriseException e) {
      response.setMessage("problems connecting with highrise " + e.toString());
    } catch (Exception e) {
      response.setMessage("problems connecting with highrise " + e.toString());
    }
  }

  public void registerHighriseUser(Credentials credentials) {
    Highrise highrise = new Highrise(credentials.getUserName(), credentials.getApiKey());
    List<Person> all = highrise.getPeopleManager().getAll(new Long(1));
  }
}