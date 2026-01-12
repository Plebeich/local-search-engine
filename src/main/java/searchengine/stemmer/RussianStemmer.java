package searchengine.stemmer;


import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RussianStemmer {
    private static final Pattern RUSSIAN_WORD_PATTERN = Pattern.compile("[а-яёА-ЯЁ]+");

    // Основной метод для получения стемов (основ слов) из текста
    public Map<String, Integer> getStems(String text) {
        Map<String, Integer> stems = new HashMap<>();

        // Очищаем текст
        String cleanText = cleanText(text);

        // Находим все русские слова
        Matcher matcher = RUSSIAN_WORD_PATTERN.matcher(cleanText);

        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (word.length() > 2) { // Игнорируем слишком короткие слова
                String stem = stem(word);
                stems.put(stem, stems.getOrDefault(stem, 0) + 1);
            }
        }

        return stems;
    }

    // Очистка текста от HTML и лишних символов
    public String cleanText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // Удаляем HTML теги
        String text = html.replaceAll("<[^>]+>", " ");

        // Удаляем скрипты и стили
        text = text.replaceAll("<script[^>]*>.*?</script>", " ");
        text = text.replaceAll("<style[^>]*>.*?</style>", " ");

        // Заменяем HTML-сущности
        text = text.replaceAll("&[a-z]+;", " ");

        // Удаляем множественные пробелы
        text = text.replaceAll("\\s+", " ");

        return text.trim();
    }

    // Упрощённый стеммер для русского языка
    private String stem(String word) {
        if (word.length() <= 3) {
            return word;
        }

        String stem = word;

        // Сначала удаляем приставки (префиксы)
        String[] prefixes = {"пере", "при", "про", "под", "над", "пред", "от", "об", "в", "со", "из", "за", "на", "до", "по", "вы", "с"};
        for (String prefix : prefixes) {
            if (stem.startsWith(prefix) && stem.length() > prefix.length() + 2) {
                stem = stem.substring(prefix.length());
                break;
            }
        }

        // Удаляем суффиксы
        String[] suffixes = {
                "ировани", "овани", "евани", "ани", "яни", "ини", "ени", "ти", "ость",
                "ация", "яция", "иция", "атор", "ятор", "итор", "аци", "яци", "ици",
                "изм", "ист", "ость", "ическ", "ическо", "ически", "изация"
        };

        for (String suffix : suffixes) {
            if (stem.endsWith(suffix) && stem.length() > suffix.length() + 2) {
                stem = stem.substring(0, stem.length() - suffix.length());
                break;
            }
        }

        // Удаляем падежные окончания
        if (stem.length() > 4) {
            String[] endings = {
                    "а", "я", "о", "е", "и", "ы", "у", "ю", "ей", "ой", "ем", "ом",
                    "ам", "ям", "ах", "ях", "ами", "ями", "ов", "ев", "ин", "ын"
            };

            for (String ending : endings) {
                if (stem.endsWith(ending) && stem.length() > ending.length() + 2) {
                    stem = stem.substring(0, stem.length() - ending.length());
                    break;
                }
            }
        }

        // Проверяем, чтобы стем не был слишком коротким
        if (stem.length() < 3) {
            return word; // Возвращаем оригинальное слово
        }

        return stem;
    }

    // Метод для получения стемов из поискового запроса
    public String[] getQueryStems(String query) {
        String cleanQuery = query.toLowerCase()
                .replaceAll("[^а-яё\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = cleanQuery.split("\\s+");
        String[] stems = new String[words.length];

        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 2) {
                stems[i] = stem(words[i]);
            } else {
                stems[i] = words[i];
            }
        }

        return stems;
    }

    // Метод для извлечения заголовка из HTML
    public String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
