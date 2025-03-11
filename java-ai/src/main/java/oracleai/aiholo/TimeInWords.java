package oracleai.aiholo;

import java.time.*;
import java.util.Map;

public class TimeInWords {

    private static final Map<String, ZoneId> TIME_ZONES = Map.of(
            "en", ZoneId.of("America/New_York"),  // US Eastern Time
            "es", ZoneId.of("Europe/Madrid"),     // Spain
            "zh", ZoneId.of("Asia/Shanghai"),     // China
            "it", ZoneId.of("Europe/Rome"),       // Italy
            "de", ZoneId.of("Europe/Berlin"),     // Germany
            "hi", ZoneId.of("Asia/Kolkata")       // India
    );

    private static final Map<Integer, String> NUMBERS_EN = Map.ofEntries(
            Map.entry(0, "Twelve"), Map.entry(1, "One"), Map.entry(2, "Two"), Map.entry(3, "Three"),
            Map.entry(4, "Four"), Map.entry(5, "Five"), Map.entry(6, "Six"), Map.entry(7, "Seven"),
            Map.entry(8, "Eight"), Map.entry(9, "Nine"), Map.entry(10, "Ten"), Map.entry(11, "Eleven")
    );

    private static final Map<Integer, String> NUMBERS_ES = Map.ofEntries(
            Map.entry(0, "Doce"), Map.entry(1, "Una"), Map.entry(2, "Dos"), Map.entry(3, "Tres"),
            Map.entry(4, "Cuatro"), Map.entry(5, "Cinco"), Map.entry(6, "Seis"), Map.entry(7, "Siete"),
            Map.entry(8, "Ocho"), Map.entry(9, "Nueve"), Map.entry(10, "Diez"), Map.entry(11, "Once")
    );

    private static final Map<Integer, String> NUMBERS_IT = Map.ofEntries(
            Map.entry(0, "Dodici"), Map.entry(1, "Uno"), Map.entry(2, "Due"), Map.entry(3, "Tre"),
            Map.entry(4, "Quattro"), Map.entry(5, "Cinque"), Map.entry(6, "Sei"), Map.entry(7, "Sette"),
            Map.entry(8, "Otto"), Map.entry(9, "Nove"), Map.entry(10, "Dieci"), Map.entry(11, "Undici")
    );

    private static final Map<Integer, String> NUMBERS_DE = Map.ofEntries(
            Map.entry(0, "Zwölf"), Map.entry(1, "Eins"), Map.entry(2, "Zwei"), Map.entry(3, "Drei"),
            Map.entry(4, "Vier"), Map.entry(5, "Fünf"), Map.entry(6, "Sechs"), Map.entry(7, "Sieben"),
            Map.entry(8, "Acht"), Map.entry(9, "Neun"), Map.entry(10, "Zehn"), Map.entry(11, "Elf")
    );

    private static final Map<Integer, String> NUMBERS_HI = Map.ofEntries(
            Map.entry(0, "बारह"), Map.entry(1, "एक"), Map.entry(2, "दो"), Map.entry(3, "तीन"),
            Map.entry(4, "चार"), Map.entry(5, "पांच"), Map.entry(6, "छह"), Map.entry(7, "सात"),
            Map.entry(8, "आठ"), Map.entry(9, "नौ"), Map.entry(10, "दस"), Map.entry(11, "ग्यारह")
    );

    private static final Map<Integer, String> MINUTES_COMMON = Map.ofEntries(
            Map.entry(0, "o'clock"), Map.entry(15, "fifteen"), Map.entry(30, "thirty"), Map.entry(45, "forty-five")
    );

    private static final Map<String, String> AM_PM_EN = Map.of("AM", "AM", "PM", "PM");
    private static final Map<String, String> AM_PM_ES = Map.of("AM", "de la mañana", "PM", "de la noche");
    private static final Map<String, String> AM_PM_IT = Map.of("AM", "di mattina", "PM", "di sera");
    private static final Map<String, String> AM_PM_DE = Map.of("AM", "Uhr morgens", "PM", "Uhr abends");
    private static final Map<String, String> AM_PM_HI = Map.of("AM", "सुबह", "PM", "शाम");

    public static String getTimeInWords(String language) {
        ZoneId zone = TIME_ZONES.getOrDefault(language.toLowerCase(), ZoneId.of("UTC"));
        LocalTime now = LocalTime.now(zone);

        int hour = now.getHour() % 12;
        int minute = now.getMinute();
        boolean isAM = now.getHour() < 12;
        if (hour == 0) hour = 12;

        return switch (language.toLowerCase()) {
            case "es" -> formatTime(NUMBERS_ES, MINUTES_COMMON, AM_PM_ES, hour, minute, isAM);
            case "it" -> formatTime(NUMBERS_IT, MINUTES_COMMON, AM_PM_IT, hour, minute, isAM);
            case "de" -> formatTime(NUMBERS_DE, MINUTES_COMMON, AM_PM_DE, hour, minute, isAM);
            case "hi" -> formatTime(NUMBERS_HI, MINUTES_COMMON, AM_PM_HI, hour, minute, isAM);
            default -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
        };
    }

    private static String formatTime(Map<Integer, String> numbers, Map<Integer, String> minutes,
                                     Map<String, String> amPm, int hour, int minute, boolean isAM) {
        String hourWord = numbers.get(hour);
        String minuteWord = minutes.getOrDefault(minute, String.valueOf(minute));
        String amPmWord = isAM ? amPm.get("AM") : amPm.get("PM");

        return hourWord + " " + minuteWord + " " + amPmWord;
    }

    public static void main(String[] args) {
        System.out.println("English: " + getTimeInWords("en"));
        System.out.println("Español: " + getTimeInWords("es"));
        System.out.println("Italiano: " + getTimeInWords("it"));
        System.out.println("Deutsch: " + getTimeInWords("de"));
        System.out.println("हिन्दी: " + getTimeInWords("hi"));
    }
}