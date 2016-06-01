package api;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.MongoDBConnection;
import db.MySQLDBConnection;

/**
 * Servlet implementation class SearchRestaurants
 */
@WebServlet("/restaurants")
public class SearchRestaurants extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// private static final long serialVersionUID = 1L;
	private static final DBConnection connection = new MySQLDBConnection();
	// private static final DBConnection connection = new MongoDBConnection();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SearchRestaurants() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		// response.getWriter().append("Served at:
		// ").append(request.getContextPath());
		/*
		 * //the first part of code in page 31
		 * response.setContentType("application/json");
		 * response.addHeader("Access­Control­Allow­Origin", "*"); String
		 * username = ""; PrintWriter out = response.getWriter(); if
		 * (request.getParameter("username") != null) { username =
		 * request.getParameter("username"); out.print("Hello " + username); }
		 * out.flush(); out.close();
		 */
		/*
		 * //the code in page 32 response.setContentType("text/html");
		 * PrintWriter out = response.getWriter(); out.println("<html><body>");
		 * out.println("<h1>This is a HTML page</h1>");
		 * out.println("</body></html>"); out.flush(); out.close();
		 */

		/*
		 * //code in page 34 response.setContentType("application/json");
		 * response.addHeader("Access­Control­Allow­Origin", "*"); String
		 * username = ""; if (request.getParameter("username") != null) {
		 * username = request.getParameter("username"); } JSONObject obj = new
		 * JSONObject(); try { obj.put("username", username); } catch
		 * (JSONException e) { e.printStackTrace(); } PrintWriter out =
		 * response.getWriter(); out.print(obj); out.flush(); out.close();
		 */

		// code in page 37
		// allow access only if session exists
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}

		JSONArray array = new JSONArray();

		if (request.getParameterMap().containsKey("user_id") && request.getParameterMap().containsKey("lat")
				&& request.getParameterMap().containsKey("lon")) {
			String userId = request.getParameter("user_id");
			double lat = Double.parseDouble(request.getParameter("lat"));
			double lon = Double.parseDouble(request.getParameter("lon")); // return
			array = connection.searchRestaurants(userId, lat, lon);
			// some
			// fake
			// restaurants
			// array.put(new JSONObject().put("name", "Panda Express"));
			// array.put(new JSONObject().put("name", "Hong Kong Express"));

			RpcParser.writeOutput(response, array);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
