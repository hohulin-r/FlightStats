package by.lomitr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FlightStats {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: java FlightStats <tickets.json>");
            return;
        }

        String filePath = args[0];

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(filePath));
            JsonNode tickets = root.get("tickets");

            if (tickets == null || !tickets.isArray()) {
                System.out.println("Ошибка: файл JSON не содержит массива tickets");
                return;
            }

            List<Flight> flights = new ArrayList<>();
            for (JsonNode ticket : tickets) {
                flights.add(parseFlight(ticket));
            }

            // 1. Минимальное время полета для каждого перевозчика
            Map<String, Long> minFlightTimes = flights.stream()
                    .filter(f -> f.origin.equals("VVO") && f.destination.equals("TLV"))
                    .collect(Collectors.groupingBy(
                            f -> f.carrier,
                            Collectors.mapping(
                                    f -> f.flightDurationMinutes(),
                                    Collectors.minBy(Long::compare)
                            )
                    ))
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(Long.MAX_VALUE)));

            System.out.println("Минимальное время полета (в минутах) по каждому перевозчику:");
            minFlightTimes.forEach((carrier, minutes) ->
                    System.out.println("Перевозчик " + carrier + ": " + minutes + " минут"));

            // 2. Разница между средней и медианной ценой

            List<Integer> prices = flights.stream()
                    .filter(f -> f.origin.equals("VVO") && f.destination.equals("TLV"))
                    .map(f -> f.price)
                    .sorted()
                    .collect(Collectors.toList());

            double avgPrice = prices.stream().mapToInt(p -> p).average().orElse(0);
            double medianPrice;
            int size = prices.size();
            if (size % 2 == 0) {
                medianPrice = (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
            } else {
                medianPrice = prices.get(size / 2);
            }

            double diff = avgPrice - medianPrice;
            System.out.printf("\nРазница между средней и медианной ценой: %.2f\n", diff);

        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        }
    }

    private static Flight parseFlight(JsonNode node) {
        String origin = node.get("origin").asText();
        String destination = node.get("destination").asText();
        String carrier = node.get("carrier").asText();
        int price = node.get("price").asInt();
        String depDate = node.get("departure_date").asText();
        String depTime = node.get("departure_time").asText();
        String arrDate = node.get("arrival_date").asText();
        String arrTime = node.get("arrival_time").asText();

        return new Flight(origin, destination, carrier, price, depDate, depTime, arrDate, arrTime);
    }

    static class Flight {
        String origin;
        String destination;
        String carrier;
        int price;
        LocalDateTime departure;
        LocalDateTime arrival;

        static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy H:mm");

        public Flight(String origin, String destination, String carrier, int price,
                      String depDate, String depTime, String arrDate, String arrTime) {
            this.origin = origin;
            this.destination = destination;
            this.carrier = carrier;
            this.price = price;
            this.departure = LocalDateTime.parse(depDate + " " + depTime, dateFormatter);
            this.arrival = LocalDateTime.parse(arrDate + " " + arrTime, dateFormatter);
        }

        public long flightDurationMinutes() {
            return Duration.between(departure, arrival).toMinutes();
        }
    }
}