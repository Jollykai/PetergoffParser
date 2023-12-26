package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.Enums.URLs;
import org.example.Models.Run;
import org.example.Models.Volunteer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    private static final Map<String, Volunteer> volunteerMap = new HashMap<>();

    public static void main(String[] args) {
        List<Run> allRuns = extractRuns(getAllRunsTable());
        getAllVolunteers(allRuns);
        saveToFile(allRuns);
        waitForAnyKey();
    }

    private static Element getAllRunsTable() {
        Document runsPage = getDocument(URLs.RUNS_PAGE.getUrl());
        return runsPage.selectFirst("table.results-table");
    }

    private static Document getDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com")
                    .get();
        } catch (IOException e) {
            log.error("Error while getting document: {}", e.getMessage());
            throw new RuntimeException();
        }
    }

    private static List<Run> extractRuns(Element tableWithAllRuns) {
        Pattern pattern = Pattern.compile("^\\d+\\s(\\d{2}\\.\\d{2}\\.\\d{4})");
        return tableWithAllRuns.select("tr")
                .stream()
                .map(Element::text)
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> {
                    long runId = Long.parseLong(matcher.group(0).split("\\s")[0]);
                    String date = matcher.group(1);
                    return new Run(runId, date);
                })
                .toList();
    }

    private static void getAllVolunteers(List<Run> runs) {
        runs.forEach(run -> {
            Elements volunteerDataRows = getVolunteerTable(run).select("tbody tr");
            log.info("Volunteers in table: {}", volunteerDataRows.size());
            Set<Volunteer> uniqueVolunteers = getUniqueVolunteers(volunteerDataRows);
            log.info("Volunteers unique: {}", uniqueVolunteers.size());
            addVolunteersToMap(uniqueVolunteers);
            log.info("Volunteers in map: {}\n", volunteerMap.size());
        });
    }

    private static Element getVolunteerTable(Run run) {
        log.info("Parsing run: {}", run.getId());
        Document pageWithRun = getDocument(URLs.PAGE_WITH_RUN.getUrl().replace("{runDate}", String.valueOf(run.getDate())));
        return pageWithRun.select("table.sortable.n-last.results-table.min-w-full.leading-normal").last();
    }

    private static Set<Volunteer> getUniqueVolunteers(Elements rows) {
        return rows.stream()
                .map(Main::extractVolunteerData)
                .collect(Collectors.toSet());
    }

    private static Volunteer extractVolunteerData(Element row) {
        Element link = row.selectFirst("a");
        String name = link != null ? link.text() : "";
        String href = link != null ? link.attr("href") : "";
        String id = extractVolunteerId(href, name);
        return new Volunteer(id, name);
    }

    private static String extractVolunteerId(String href, String name) {
        String id = href.substring(href.lastIndexOf('/') + 1);
        if (isUnregisteredVolunteer(id)) {
            id = "UNREG: " + name;
        }
        return id;
    }

    private static boolean isUnregisteredVolunteer(String id) {
        return id.equals("?utm_source=5verst&utm_medium=results&utm_campaign=regular");
    }

    private static void addVolunteersToMap(Set<Volunteer> volunteers) {
        for (Volunteer volunteer : volunteers) {
            volunteerMap.computeIfPresent(volunteer.getId(), (id, existingVolunteer) -> {
                existingVolunteer.incrementCounter();
                return existingVolunteer;
            });
            volunteerMap.putIfAbsent(volunteer.getId(), volunteer);
        }
    }

    private static void saveToFile(List<Run> runs) {
        String fileName = generateFileName(runs.get(0).getDate());
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            String header = "ID;name;COUNT;;Last run #" + runs.size() + " - Date: " + runs.get(0).getDate();
            writer.println(header);
            volunteerMap.values().stream()
                    .sorted(Comparator.comparingInt(Volunteer::getCounter).reversed())
                    .forEach(writer::println);
        } catch (IOException e) {
            log.error("Error while writing file: {}", e.getMessage());
        }
    }

    private static String generateFileName(String date) {
        return "volunteers_up_to_" + date + ".csv";
    }

    private static void waitForAnyKey() {
        System.out.println("Press any key to continue...");
        Scanner input = new Scanner(System.in);
        input.nextLine();
    }
}