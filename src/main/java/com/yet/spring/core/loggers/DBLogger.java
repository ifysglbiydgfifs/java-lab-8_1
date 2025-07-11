package com.yet.spring.core.loggers;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.yet.spring.core.beans.Event;

public class DBLogger extends AbstractLogger {

    private final JdbcTemplate jdbcTemplate;

    public DBLogger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTableIfNotExists();
        updateEventAutoId();
    }

    public void destroy() {
        int totalEvents = getTotalEvents();
        System.out.println("Total events in the DB: " + totalEvents);

        List<Event> allEvents = getAllEvents();
        String allEventIds = allEvents.stream()
                .map(Event::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        System.out.println("All DB Event ids: " + allEventIds);
    }

    private void createTableIfNotExists() {
        try {
            jdbcTemplate.update("""
                CREATE TABLE IF NOT EXISTS T_EVENT (
                    id INT PRIMARY KEY,
                    date TIMESTAMP,
                    msg VARCHAR(255)
                )
            """);
            System.out.println("Checked/created table T_EVENT in PUBLIC schema.");
        } catch (DataAccessException e) {
            System.err.println("Failed to create table T_EVENT: " + e.getMessage());
            throw e;
        }
    }

    private void updateEventAutoId() {
        int maxId = getMaxId();
        Event.initAutoId(maxId + 1);
        System.out.println("Initialized Event.AUTO_ID to " + maxId);
    }

    private int getMaxId() {
        Integer count = jdbcTemplate.queryForObject("SELECT MAX(id) FROM T_EVENT", Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public void logEvent(Event event) {
        createTableIfNotExists();
        jdbcTemplate.update("INSERT INTO T_EVENT (id, date, msg) VALUES (?, ?, ?)",
                event.getId(), event.getDate(), "[DB] " + event);
        System.out.println("Saved to DB event with id " + event.getId());
    }

    public int getTotalEvents() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM T_EVENT", Integer.class);
        return count != null ? count : 0;
    }

    public List<Event> getAllEvents() {
        return jdbcTemplate.query("SELECT * FROM T_EVENT", new RowMapper<Event>() {
            @Override
            public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
                int id = rs.getInt("id");
                Date date = rs.getDate("date");
                String msg = rs.getString("msg");
                return new Event(id, new Date(date.getTime()), msg);
            }
        });
    }
}
