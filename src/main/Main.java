package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Map;
import java.util.Properties;

@WebServlet("/v.1.0/")
public class Main extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";
    private static final String HOST;
    private static final String LOGIN;
    private static final String PASSWORD;
    private static final String CONFIG_FILE = "src/main/resources/properties.conf";
    static {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(CONFIG_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        HOST = properties.getProperty("HOST");
        LOGIN = properties.getProperty("SITE_LOGIN");
        PASSWORD = properties.getProperty("PASSWORD");
    }
	private static final String ALL_TASKS_PAGE = HOST + "tech/all.php";
	private static final String LOGIN_REQUEST = HOST + "?login=yes&backurl=%2F&AUTH_FORM=Y&TYPE=AUTH&Login=Войти&AUTH_FORM=Y&TYPE=AUTH&USER_LOGIN=" + LOGIN + "&USER_PASSWORD=" + PASSWORD + "&Login=Войти";
	private static final String[] crashTasks = {};

	public Main() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter w = response.getWriter();
		Map<String, String> cook = Main.getCookies();
		ArrayList<Task> newTasks;
		newTasks = getTasks(cook);
		ArrayList<Task> shortTasks = getShortESKCount(newTasks);
		int shortTasksSize = shortTasks.size();
		int secondsToDeath;
		if (shortTasksSize != 0) {
			try {
				secondsToDeath = getSecondsToDeath(shortTasks);
				w.append("{\"shortTask\" : \"" + shortTasksSize + "\",\"secondsToDeath\" : \"" + secondsToDeath + "\"}");
			} catch (ParseException e) {
				secondsToDeath = 0;
			} 
		} else {
			w.append("{\"shortTask\" : \"" + shortTasksSize + "\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private static Map<String, String> getCookies() throws IOException {
		Connection.Response response = Jsoup.connect(LOGIN_REQUEST)
				.method(Connection.Method.POST).execute();
		return response.cookies();
	}

	private static ArrayList<Task> getTasks(Map<String, String> cookies) throws IOException {

		ArrayList<Task> tasks = new ArrayList<Task>();
		int pagesCount = getPagesCount(cookies);

		for (int j = 1; j <= pagesCount; j++) {
			Document newTasks = Jsoup.connect(ALL_TASKS_PAGE + "?page=" + j).cookies(cookies).get();

			Element table = newTasks.select("tbody").get(0);
			Elements rows = table.select("tr");

			for (int i = 0; i < rows.size(); i++) { // first row is the col names so skip it.
				Element row = rows.get(i);
				String techService = row.select("td.col_nomer").get(0).text();
				String typeService = row.getElementsByClass("hiddable2").get(0).text();
				String dateEnd = row.getElementsByClass("col_dateend").get(0).text();
				String status = row.getElementsByClass("col_stage_name").text();

				tasks.add(new Task(typeService, dateEnd, status, techService));
			}
		}
		return tasks;
	}

	private static ArrayList<Task> getShortESKCount(ArrayList<Task> list) {
		ArrayList<Task> al = new ArrayList<>();
		for (Task x : list) {
			if (x.getTypeService().startsWith("5.13.3(1)") && !x.getStatus().equals("Завершено") && !isContains(x.getTechService(), crashTasks)) {
				al.add(x);
			}
		}
		return al;
	}

	private static int getSecondsToDeath(ArrayList<Task> list) throws ParseException {
		Date now = new Date();
		Date minDate = getDateFromString(list.get(0).getDateEnd());
		for (Task task : list) {
			Date taskDate = getDateFromString(task.getDateEnd());
			if (minDate.after(taskDate)) {
				minDate = taskDate;
			}
		}
		return (int) (minDate.getTime() - now.getTime()) / 1000;
	}

	private static Date getDateFromString(String string) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
		return format.parse(string);
	}

	private static int getPagesCount(Map<String, String> cookies) throws IOException {
		Document allTasks = Jsoup.connect(ALL_TASKS_PAGE).cookies(cookies).get();
		Elements pages = allTasks.getElementsByClass("pagination").select("li");
		return pages.size();
	}

	private static boolean isContains(String tu, String[] crashTu){
		if (Arrays.asList(crashTu).contains(tu)) return true;
		else return false;
	}
}
