package com.neu.wham.services;


import java.util.List;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.neu.wham.dao.EventDAO;
import com.neu.wham.model.Event;
import com.neu.wham.model.EventbritePreferences;


@Service
public class GetEventServiceImpl implements GetEventService {
	
	@Autowired
	private EventDAO eventDAO;
	
	@Autowired
	private PreferenceService prefService;
	
	@Override
	//public List<Event> getEvents(String lat, String lon, String rad, String q, String statDT, String endDT,
	//		String[] formats, String[] categories, String[] subcategories)
	public List<Event> getEvents(HashMap<String, String> params)
	{
		// set up event lists
		List<Event> DBEvents = new ArrayList<Event>();
		List<Event> APIEvents = new ArrayList<Event>();
		List<Event> NEUEvents = new ArrayList<Event>();
		List<Event> resultList = new ArrayList<Event>();
		
		// read the params
		String lat = params.get("lat");
		String lon = params.get("lon");
		String rad = params.get("rad");
		String q = params.get("q");
		String statDT = params.get("statDT");
		String endDT = params.get("endDT");
		String userId = params.get("userId");
		
		// build the Eventbrite preferences
		EventbritePreferences ePrefs = new EventbritePreferences();
		if(null != userId) {
			if(prefService == null)
				System.out.println("yup");
			ePrefs = prefService.buildEventbritePreferences(userId);
		}
			
 		try
		{
	 		APIEvents = getEventsFromAPI(lat, lon, rad, q, statDT, endDT, ePrefs.getFormats(), ePrefs.getCategories(), ePrefs.getSubcategories());	
			DBEvents =  eventDAO.getEventsData(lat, lon, rad);
	//		NEUEvents = getNEUEvents();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
 		resultList.addAll(APIEvents);
		resultList.addAll(DBEvents);
//		resultList.addAll(NEUEvents);
 		
		return resultList;
	}
	
	public List<Event> getEventsFromAPI(String lat, String lon, String radius, String q, 
			String statDT, String endDT, String[] formats, String[] categories, String[] subcategories) 
			throws UnirestException, JSONException, ParseException, URISyntaxException
	{
		System.out.println("in getEventsFromAPI");
		URIBuilder builder = new URIBuilder("https://www.eventbriteapi.com/v3/events/search");
		builder.addParameter("expand", "venue");
		builder.addParameter("location.latitude", lat);
		builder.addParameter("location.longitude", lon);
		builder.addParameter("location.within", radius + "mi");
		builder.addParameter("token", "DXVHSQKC2T2GGBTUPOY2");
		if(null != q)
			builder.addParameter("q", q);
		if(null != statDT){
			System.out.println("XIWANG");
			System.out.println("statDT in string:" + statDT);
			builder.addParameter("start_date.range_start", statDT);
		}
		if(null != endDT){
			System.out.println("endDT in string:" + endDT);
			builder.addParameter("start_date.range_end", endDT);
		}
		if(null != formats && formats.length > 0)
			builder.addParameter("formats", String.join(",", formats));
		if(null != categories && categories.length > 0)
			builder.addParameter("categories", String.join(",", categories));
		if(null != subcategories && subcategories.length > 0)
			builder.addParameter("subcategories", String.join(",", subcategories));
		
		System.out.println(builder);
		System.out.println(builder.toString());
		
		HttpResponse<JsonNode> jsonResponse = Unirest.get(builder.toString()).asJson();
		System.out.println(jsonResponse.getStatus());
		System.out.println("*****");
		
		JsonNode obj = jsonResponse.getBody();
		JSONObject response = obj.getObject();
		JSONArray events = response.getJSONArray("events");
		
		List<Event> eventList = new ArrayList<Event>();
		
		for(int i = 0; i < events.length(); i++){
			
			Event e = new Event();
			
			JSONObject event = events.getJSONObject(i);
			String eventName = event.getJSONObject("name").getString("text");
			e.setEventName(eventName);
			String eventDesc = event.getJSONObject("description").getString("text");
			e.setEventDesc(eventDesc);
			String eventLocation = event.getJSONObject("venue").getJSONObject("address").getString("address_1")+ " " + 
					event.getJSONObject("venue").getJSONObject("address").getString("address_2")+ " " +
					event.getJSONObject("venue").getJSONObject("address").getString("city")+ " " +
					event.getJSONObject("venue").getJSONObject("address").getString("region")+ " " +
					event.getJSONObject("venue").getJSONObject("address").getString("postal_code")+ " " +
					event.getJSONObject("venue").getJSONObject("address").getString("country");
			e.setEventLocation(eventLocation);
			e.setPhoneNumber(null);
			e.setEmailId(null);
			
			String startDateTime = event.getJSONObject("start").getString("local");
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			TimeZone tz = TimeZone.getTimeZone(event.getJSONObject("start").getString("timezone"));
			formatter.setTimeZone(tz);
			Date startDate = formatter.parse(startDateTime);
			e.setStartDateAndTime(startDate);
			
			String endDateString = event.getJSONObject("end").getString("local");
			Date endDate = formatter.parse(endDateString);
			e.setEndDateAndTime(endDate);
			
			Double venueLat = event.getJSONObject("venue").getJSONObject("address").getDouble("latitude");
			e.setLatitude(venueLat);
			Double venueLong = event.getJSONObject("venue").getJSONObject("address").getDouble("longitude");
			e.setLongitude(venueLong);
			
			
			String creationTimeString = event.getString("created");
			Date creationTime = formatter.parse(creationTimeString);
			e.setCreationTime(creationTime);
			
			String lastUpdateTimeString = event.getString("changed");
			Date lastUpdateTime = formatter.parse(lastUpdateTimeString);
			e.setLastUpdateTime(lastUpdateTime);
			
			e.setOrganiserName(null);
			e.setOrganiserDesc(null);
			e.setOfficialEvent(false);
			e.setFilePath(null);
			
			
			eventList.add(e);
		}
		
		return eventList;
	}

	
	public List<Event> getNEUEvents() throws URISyntaxException, UnirestException, IOException, JSONException, ParserConfigurationException, SAXException, TransformerException
	{
		List<Event> NEUCalenderEvents = new ArrayList<Event>();
		
		URL url = new URL("http://calendar.northeastern.edu/widget/view?schools=northeastern&days=20&num=50&all_instances=1&format=xml");
		URLConnection conn = url.openConnection();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(conn.getInputStream());

//		TransformerFactory Tfactory = TransformerFactory.newInstance();
//		Transformer xform = Tfactory.newTransformer();
//
//		xform.transform(new DOMSource(doc), new StreamResult(System.out));
	    
		NodeList itemList = doc.getElementsByTagName("item");
		
//		System.out.println("Length:" + itemList.getLength());
		
		for (int i=0; i < itemList.getLength(); i++)
		{
			Node nNode = itemList.item(i);
			Element eElement = (Element) nNode;	
			
			try
			{
				Event e = new Event();
				if(eElement.getElementsByTagName("title").getLength() != 0)
					e.setEventName(eElement.getElementsByTagName("title").item(0).getTextContent());
				if(eElement.getElementsByTagName("description").getLength() != 0)
					e.setEventDesc(eElement.getElementsByTagName("description").item(0).getTextContent());
				if(eElement.getElementsByTagName("geo:lat").getLength() != 0)
					e.setLatitude((double)Double.parseDouble(eElement.getElementsByTagName("geo:lat").item(0).getTextContent()));
				if(eElement.getElementsByTagName("geo:lng").getLength() != 0)
					e.setLongitude((double)Double.parseDouble(eElement.getElementsByTagName("geo:lng").item(0).getTextContent()));
				e.setOfficialEvent(true);
				
				NEUCalenderEvents.add(e);
			}
			catch(Exception e)
			{
				System.out.println("Error:" + e.getStackTrace());
			}
		}
		
		System.out.println("NEU Events:" + NEUCalenderEvents.size());
		
		return NEUCalenderEvents;
		
	}
}
