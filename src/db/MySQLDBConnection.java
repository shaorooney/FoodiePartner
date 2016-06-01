package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Restaurant;

import org.json.JSONArray;
import org.json.JSONObject;

import yelp.YelpAPI;

public class MySQLDBConnection implements DBConnection {
	private static final int MAX_RECOMMENDED_RESTAURANTS = 20;
	private Connection conn;

	public MySQLDBConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(DBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean executeUpdateStatement(String query) {// handling exception
															// for update query
		if (conn == null) {
			return false;
		}
		try {
			Statement stmt = conn.createStatement();
			System.out.println("\nDBConnection executing query:\n" + query);
			stmt.executeUpdate(query);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private ResultSet executeFetchStatement(String query) {// handling exception
															// for select query
		if (conn == null) {
			return null;
		}
		try {
			Statement stmt = conn.createStatement();
			System.out.println("\nDBConnection executing query:\n" + query);
			return stmt.executeQuery(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public JSONArray searchRestaurants(String userId, double lat, double lon) {
		// search and some restaurants, store into restaurants table.
		try {
			YelpAPI api = new YelpAPI();
			JSONObject response = new JSONObject(api.searchForBusinessesByLocation(lat, lon));
			JSONArray array = (JSONArray) response.get("businesses");

			List<JSONObject> list = new ArrayList<>();
			Set<String> visited = getVisitedRestaurants(userId);

			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				Restaurant restaurant = new Restaurant(object);
				String businessId = restaurant.getBusinessId();
				String name = restaurant.getName();
				String categories = restaurant.getCategories();
				String city = restaurant.getCity();
				String state = restaurant.getState();
				String fullAddress = restaurant.getFullAddress();
				double stars = restaurant.getStars();
				double latitude = restaurant.getLatitude();
				double longitude = restaurant.getLongitude();
				String imageUrl = restaurant.getImageUrl();
				String url = restaurant.getUrl();
				JSONObject obj = restaurant.toJSONObject();
				if (visited.contains(businessId)) {
					obj.put("is_visited", true);
				} else {
					obj.put("is_visited", false);
				}
				executeUpdateStatement("INSERT IGNORE INTO restaurants " + "VALUES ('" + businessId + "', \"" + name
						+ "\", \"" + categories + "\", \"" + city + "\", \"" + state + "\", " + stars + ", \""
						+ fullAddress + "\", " + latitude + "," + longitude + ",\"" + imageUrl + "\", \"" + url
						+ "\")");
				list.add(obj);

			}
			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/*
	 * @Override public void close() { // TODO Auto-generated method stub
	 * 
	 * }
	 */

	@Override
	public boolean setVisitedRestaurants(String userId, List<String> businessIds) {// update
																					// visit
																					// history
		boolean result = true;
		for (String businessId : businessIds) {
			if (!executeUpdateStatement("INSERT INTO history (`user_id`, `business_id`) VALUES (\"" + userId + "\", \""
					+ businessId + "\")")) {
				result = false;
			}
		}
		return result;
	}

	@Override
	public boolean unsetVisitedRestaurants(String userId, List<String> businessIds) {// delete
																						// from
																						// visit
																						// history
		boolean result = true;
		for (String businessId : businessIds) {
			if (!executeUpdateStatement("DELETE FROM history WHERE `user_id`=\"" + userId + "\" and `business_id` = \""
					+ businessId + "\"")) {
				result = false;
			}
		}
		return result;
	}

	@Override
	public Set<String> getVisitedRestaurants(String userId) { // tell whether
																// this user has
																// visited one
																// particular
																// restaurant
		Set<String> visitedRestaurants = new HashSet<String>();
		try {
			String sql = "SELECT business_id from history WHERE user_id=" + userId;
			ResultSet rs = executeFetchStatement(sql);
			while (rs.next()) {
				String visitedRestaurant = rs.getString("business_id");
				visitedRestaurants.add(visitedRestaurant);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return visitedRestaurants;
	}

	@Override
	public JSONObject getRestaurantsById(String businessId, boolean isVisited) {
		// TODO Auto-generated method stub
		try {
			String sql = "SELECT * from " + "restaurants where business_id='" + businessId + "'"
					+ " ORDER BY stars DESC";
			ResultSet rs = executeFetchStatement(sql);
			if (rs.next()) {
				Restaurant restaurant = new Restaurant(rs.getString("business_id"), rs.getString("name"),
						rs.getString("categories"), rs.getString("city"), rs.getString("state"),
						rs.getString("full_address"), rs.getFloat("stars"), rs.getFloat("latitude"),
						rs.getFloat("longitude"), rs.getString("image_url"), rs.getString("url"));
				JSONObject obj = restaurant.toJSONObject();
				obj.put("is_visited", isVisited); // front end field to show
													// visited
				return obj;
			}
		} catch (Exception e) { /* report an error */
			System.out.println(e.getMessage());
		}
		return null;

	}

	@Override
	public JSONArray recommendRestaurants(String userId) {
		try {
			// step 1
			Set<String> visitedRestaurants = getVisitedRestaurants(userId);
			// step 2
			Set<String> allCategories = new HashSet<>();// why hashSet?
														// de-duplicate
			for (String restaurant : visitedRestaurants) {
				allCategories.addAll(getCategories(restaurant));
			}
			// step 3
			Set<String> allRestaurants = new HashSet<>();
			for (String category : allCategories) {
				Set<String> set = getBusinessId(category);
				allRestaurants.addAll(set);
			}
			// step 4
			Set<JSONObject> diff = new HashSet<>();
			int count = 0; // the # of restaurants has been recommended
			for (String businessId : allRestaurants) {
				// Perform filtering
				if (!visitedRestaurants.contains(businessId)) {
					diff.add(getRestaurantsById(businessId, false));
					count++;
					if (count >= MAX_RECOMMENDED_RESTAURANTS) {
						break;
					}
				}
			}
			// step 5: advanced improve order by distance??

			return new JSONArray(diff);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	@Override
	public Set<String> getCategories(String businessId) {
		// given businessId, get categories
		try {
			String sql = "SELECT categories from restaurants WHERE business_id='" + businessId + "'";
			ResultSet rs = executeFetchStatement(sql);
			if (rs.next()) {
				Set<String> set = new HashSet<>();
				String[] categories = rs.getString("categories").split(",");
				for (String category : categories) {
					// ' Japanese ' -> 'Japanese'
					set.add(category.trim());
				}
				return set;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return new HashSet<String>();

	}

	@Override
	public Set<String> getBusinessId(String category) {
		// given category, get all businessId
		Set<String> set = new HashSet<>();
		try {
			// if category = Chinese, categories = Chinese, Korean, Japanese,
			// it's a match
			String sql = "SELECT business_id from restaurants WHERE categories LIKE '%" + category + "%'";
			ResultSet rs = executeFetchStatement(sql);
			while (rs.next()) {
				String businessId = rs.getString("business_id");
				set.add(businessId);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return set;

	}

	@Override
	public Boolean verifyLogin(String userId, String password) {
		try {
			if (conn == null) {
				return false;
			}
			// String sql = "SELECT user_id from users WHERE user_id='" + userId
			// + "' and password='" + password + "'";
			// ResultSet rs = executeFetchStatement(sql);
			// use prepareStatement() to fix SQL injection attack.
			String sql = "SELECT user_id from users WHERE user_id=? and password=?";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, userId);
			pstmt.setString(2, password);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;

	}

	@Override
	public String getFirstLastName(String userId) {
		String name = "";
		try {
			if (conn != null) {
				String sql = "SELECT first_name, last_name from users WHERE user_id='" + userId + "'";
				ResultSet rs = executeFetchStatement(sql);
				if (rs.next()) {
					name += rs.getString("first_name") + " " + rs.getString("last_name");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return name;

	}
}