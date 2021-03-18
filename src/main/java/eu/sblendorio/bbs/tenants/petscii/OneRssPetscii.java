package eu.sblendorio.bbs.tenants.petscii;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import eu.sblendorio.bbs.core.BbsThread;
import eu.sblendorio.bbs.core.HtmlUtils;
import static eu.sblendorio.bbs.core.PetsciiColors.GREY3;
import static eu.sblendorio.bbs.core.PetsciiColors.WHITE;
import static eu.sblendorio.bbs.core.PetsciiKeys.CASE_LOCK;
import static eu.sblendorio.bbs.core.PetsciiKeys.CLR;
import static eu.sblendorio.bbs.core.PetsciiKeys.DEL;
import static eu.sblendorio.bbs.core.PetsciiKeys.HOME;
import static eu.sblendorio.bbs.core.PetsciiKeys.LOWERCASE;
import static eu.sblendorio.bbs.core.PetsciiKeys.RETURN;
import static eu.sblendorio.bbs.core.PetsciiKeys.REVOFF;
import static eu.sblendorio.bbs.core.PetsciiKeys.REVON;
import static eu.sblendorio.bbs.core.PetsciiKeys.RIGHT;
import static eu.sblendorio.bbs.core.PetsciiKeys.SPACE_CHAR;
import eu.sblendorio.bbs.core.PetsciiThread;
import java.io.File;
import java.io.FileNotFoundException;
import static java.lang.Math.min;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collections;
import static java.util.Collections.emptyList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import org.apache.commons.text.WordUtils;

public class OneRssPetscii extends PetsciiThread {

    String HR_TOP = StringUtils.repeat(chr(163), getScreenColumns() - 1);

    protected int screenRows = 19;
    protected int pageSize = 10;

    protected boolean showAuthor = false;
    protected boolean newlineAfterDate = true;
    protected boolean twoColumns = true;

    protected List<NewsFeed> posts = emptyList();
    protected int currentPage = 1;

    protected boolean alwaysRefreshFeed = false;

    static class NewsSection {
        final String title;
        final Object url;

        public NewsSection(String title, Object url) {
            this.title = title; this.url = url;
        }
    }

    static class NewsFeed {
        final Date publishedDate;
        final String title;
        final String description;
        final String uri;
        final String author;

        public NewsFeed(Date publishedDate, String title, String description, String uri, String author) {
            this.publishedDate = publishedDate; this.title = title; this.description = description; this.uri = uri; this.author = author;
        }

        public String toString() {
            return "Title: "+title+"\nDate:"+publishedDate+"\nDescription:"+description+"\n";
        }
    }

    private Map<String, NewsSection> sections;

    private void readSections() throws Exception {
        final String filename = System.getProperty("MENUMES", "/data/a.txt");
        List<String> secTxt = readTxt(filename);
        sections = new LinkedHashMap<>();
        Map<String, String> config = secTxt.stream()
            .filter(row -> isNotBlank(trim(row)))
            .map(StringUtils::trim)
            .filter(row -> !row.startsWith(";"))
            .map(row -> row.replaceAll("\\s*#\\s*", "#"))
            .map(row -> row.split("#"))
            .collect(toMap(rows -> rows[0], rows -> rows[1], (a,b) -> b, LinkedHashMap::new));
        final String commands = "1234567890abcdefghijklmnopqrstuvwxyz";
        int count = 0;
        for (Map.Entry<String, String> row : config.entrySet()) {
            ++count;
            sections.put(commands.substring(count - 1, count), new NewsSection(row.getKey(), row.getValue()));
        }
        sections.put(commands.substring(count, ++count), new NewsSection("Download", new OneDownload()));
        //sections.put(commands.substring(count, ++count), new NewsSection("Connect 4", new ConnectFour()));

        // legacy:
        // sections.put("1", new NewsSection("Articoli", "https://www.labaya-make-an-offer.com/articles.xml"));
        // sections.put("2", new NewsSection("Downloads", "https://www.labaya-make-an-offer.com/download.xml"));
        // sections.put("3", new NewsSection("Tips & Tricks", "https://www.labaya-make-an-offer.com/tips%26tricks.xml"));
        // sections.put("4", new NewsSection("Tic Tac Toe", new TicTacToe()));
    }

    private void printChannelListOneColumn() {
        gotoXY(0, 6);
        int maxLen = sections.values().stream().map(x -> x.title).map(String::length).mapToInt(v -> v+4).max().orElse(0);
        String spaces = StringUtils.repeat(" ", (getScreenColumns() - maxLen) / 2);
        for (Map.Entry<String, NewsSection> entry: sections.entrySet()) {
            print(spaces); write(REVON); print(" " + entry.getKey()+ " ");
            write(REVOFF); println(" " + entry.getValue().title);
            newline();
        }
        print(spaces); write(REVON); print(" . "); write(REVOFF); print(" Exit ");
        flush();
    }

    private void printChannelListTwoColumns() {
        gotoXY(0, 5);
        List<String> keys = new LinkedList<>(sections.keySet());
        Collections.sort(keys);
        int size = sections.size() / 2;
        if (size * 2 < sections.size())
            ++size;
        for (int i=0; i<size; ++i) {
            int even = i;
            if (even >= keys.size()) break;
            String key = keys.get(even);
            NewsSection value = sections.get(key);
            write(RIGHT, GREY3, REVON, SPACE_CHAR);
            print(key); write(SPACE_CHAR, REVOFF, SPACE_CHAR);
            String title = substring(value.title + "                    ", 0, 15);
            print(title);
            print(" ");

            int odd = even + size;
            if (odd < keys.size()) {
                key = keys.get(odd);
                value = sections.get(key);
                write(GREY3, REVON, SPACE_CHAR);
                print(key);
                write(SPACE_CHAR, REVOFF, SPACE_CHAR);
                print(value.title);
            }
            if (i != size -1) {
                newline();
                if (size <= 8) newline();
            } else if (sections.size() % 2 == 0) {
                newline();
                if (size <= 8) newline();
            }

        }
        if (sections.size() % 2 == 0) write(RIGHT);
        write(GREY3, REVON, SPACE_CHAR);
        print(".");
        write(SPACE_CHAR, REVOFF, SPACE_CHAR);
        print("Exit");
        write(GREY3, RETURN, RETURN);
        flush();
    }

    public static List<NewsFeed> getFeeds(String urlString) throws Exception {
        URL url = new URL(urlString);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url));
        List<NewsFeed> result = new ArrayList<>();
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry e : entries)
            result.add(new NewsFeed(e.getPublishedDate(),
                e.getTitle().replace("\u00a0", " "),
                e.getDescription().getValue(),
                e.getUri(),
                e.getAuthor().replace("\u00a0", " ")
            ));
        return result;
    }

    @Override
    public void doLoop() throws Exception {
        cls();
        write(GREY3);
        waitOn();
        readSections();
        waitOff();
        if (sections.size() == 1) {
            enterSection(sections.values().stream().findFirst().get());
            return;
        }
        while (true) {
            write(WHITE, CLR, LOWERCASE, CASE_LOCK);
            write(LOGO_MENU);
            write(GREY3);
            posts = null;
            currentPage = 1;
            if (twoColumns)
                printChannelListTwoColumns();
            else
                printChannelListOneColumn();
            boolean isValidKey;
            int key;
            String input;
            do {
                resetInput();
                key = readKey();
                input = chr(key) + "";
                isValidKey = (sections.keySet().stream().map(String::toLowerCase).collect(toSet()).contains(input.toLowerCase())) || key == '.';
            } while (!isValidKey);
            if (key == '.') break;
            NewsSection section = sections.get(input.toLowerCase());
            enterSection(section);
        }
    }

    private void enterSection(NewsSection section) throws Exception {
        listPosts(section);
        if (section.url instanceof BbsThread)
            return;

        while (true) {
            log("RssReader waiting for input");
            write(WHITE); print("#"); write(GREY3); print(", ["); write(WHITE); print("N+-"); write(GREY3); print("]Page [");
            write(WHITE); print("R"); write(GREY3); print("]eload [");
            write(WHITE); print("."); write(GREY3); print("]"); write(WHITE); print("Q"); write(GREY3); print("uit> ");
            resetInput();
            flush();
            String inputRaw = readLine();
            String input = lowerCase(trim(inputRaw));
            if (".".equals(input) || "exit".equals(input) || "quit".equals(input) || "q".equals(input)) {
                break;
            } else if (("+".equals(input) || "n".equals(input)) && currentPage*pageSize<posts.size()) {
                ++currentPage;
                if (alwaysRefreshFeed) posts = null;
                try {
                    listPosts(section);
                } catch (NullPointerException e) {
                    --currentPage;
                    if (alwaysRefreshFeed) posts = null;
                    listPosts(section);
                }
            } else if ("-".equals(input) && currentPage > 1) {
                --currentPage;
                if (alwaysRefreshFeed) posts = null;
                listPosts(section);
            } else if ("--".equals(input)) {
                currentPage = 1;
                if (alwaysRefreshFeed) posts = null;
                listPosts(section);
            } else if ("r".equals(input) || "reload".equals(input) || "refresh".equals(input)) {
                posts = null;
                listPosts(section);
            } else if (toInt(input) >= 1 && toInt(input) <= posts.size()) {
                boolean exitByUser = displayPost(posts.get(toInt(input) - 1), section);
                if (exitByUser) listPosts(section);
            } else if ("".equals(input)) {
                listPosts(section);
            }
        }
    }

    protected void listPosts(NewsSection section) throws Exception {
        if (section.url instanceof BbsThread) {
            launch((BbsThread) section.url);
            return;
        }
        cls();
        gotoXY(28,2); write(WHITE); print(section.title);
        write(HOME); write(LOGO_SECTION);
        write(GREY3);
        if (isEmpty(posts)) {
            waitOn();
            posts = getFeeds(section.url.toString());
            waitOff();
        }

        final int start = pageSize * (currentPage-1);
        final int end = min(pageSize + start, posts.size());

        for (int i = start; i < end; ++i) {
            NewsFeed post = posts.get(i);
            write(WHITE); print((i+1) + "."); write(GREY3);
            final int iLen = 37-String.valueOf(i+1).length();
            String line = WordUtils.wrap(filterPrintable(HtmlUtils.htmlClean(post.title)), iLen, "\r", true);
            println(line.replaceAll("\r", "\r " + repeat(" ", 37-iLen)));
        }
        newline();
        flush();
    }

    private boolean displayPost(NewsFeed feed, NewsSection section) throws Exception {
        logo(section);
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        final String author = (!showAuthor || isBlank(trim(feed.author))) ? EMPTY : (" - by " + trim(feed.author));
        final String head = trim(feed.title) + author + "<br>" + HR_TOP + "<br>";
        List<String> rows = wordWrap(head);
        List<String> article = wordWrap(
            (feed.publishedDate == null ? "" : dateFormat.format(feed.publishedDate) + " - " + (newlineAfterDate ? "<br>" : ""))
                + feed.description.replaceAll("^[\\s\\n\\r]+|^(<(br|p|div)[^>]*>)+", EMPTY)
        );
        rows.addAll(article);

        int page = 1;
        int j = 0;
        boolean forward = true;
        while (j < rows.size()) {
            if (j>0 && j % screenRows == 0 && forward) {
                println();
                write(WHITE);
                print("-PAGE " + page + "-  SPACE=NEXT  -=PREV  .=EXIT");
                write(GREY3);
                flush(); resetInput(); int ch = readKey();
                if (ch == '.') {
                    return true;
                } else if (ch == '-' && page > 1) {
                    j -= (screenRows *2);
                    --page;
                    forward = false;
                    logo(section);
                    continue;
                } else {
                    ++page;
                }
                logo(section);
            }
            String row = rows.get(j);
            println(row);
            forward = true;
            ++j;
        }
        println();
        return false;
    }

    private void logo(NewsSection section) throws Exception {
        cls();
        gotoXY(28,2);
        write(WHITE);
        print(section.title);
        write(HOME);
        write(LOGO_SECTION);
        write(GREY3);
    }

    protected List<String> wordWrap(String s) {
        String[] cleaned = filterPrintableWithNewline(HtmlUtils.htmlClean(s)).split("\n");
        List<String> result = new ArrayList<>();
        for (String item: cleaned) {
            String[] wrappedLine = WordUtils
                .wrap(item, getScreenColumns() - 1, "\n", true)
                .split("\n");
            result.addAll(asList(wrappedLine));
        }
        return result;
    }

    public final static byte[] LOGO_MENU = readBinaryFile("petscii/baya-bbs.seq");

    public final static byte[] LOGO_SECTION = readBinaryFile("petscii/baya.seq");

    protected void waitOn() {
        print("WAIT PLEASE...");
        flush();
    }

    protected void waitOff() {
        for (int i=0; i<14; ++i) write(DEL);
        flush();
    }

    private List<String> readTxt(String filename) {
        List<String> result = new LinkedList<>();
        try {
            File myObj = new File(filename);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                result.add(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return result;
    }

}
