import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static final String SYSTEM_DIRECTORY = System.getProperty("user.dir");

    public static void main(String[] args) throws IOException {
        final String outputFile = "Output.txt";
        System.setOut(new PrintStream(new File(outputFile)));
        final String configFileName = "Config.txt";
        final AtomicReference<Double> epsilonMinutes = new AtomicReference<>();
        final AtomicReference<String> inputFormat = new AtomicReference<>();
        final AtomicReference<String> outputFormat = new AtomicReference<>();
        FileInsideFolderFormatter formatter = (file) ->
        {
            SimpleDateFormat df = new SimpleDateFormat(outputFormat.get());
            return file.destination + " " + df.format(file.date);
        };

        ArrayList<Class> classes = new ArrayList<>();
        ArrayList<String> fileExtensions = new ArrayList<>();

        System.out.println("Reading config files");
        readConfigFile(getScanner(configFileName), classes, fileExtensions, epsilonMinutes, inputFormat, outputFormat);

        System.out.println("Done reading config file");
        ArrayList<FileToMove> files = new ArrayList<>();
        System.out.println("Getting files in folder");
        getFilesInFolder(configFileName, inputFormat.get(), fileExtensions, files);
        System.out.println("Done getting files in folder");

        System.out.println("Before assigning destinations");
        printFiles(files);

        System.out.println("Assigning destinations");
        assignDestinations(classes, files, epsilonMinutes.get());

        sortFiles(files);

        System.out.println("Before moving");
        printFiles(files);
        moveFiles(files, formatter);
    }

    private static void printFiles(List<FileToMove> files) {
        System.out.printf("Printing %d file%s\n", files.size(), files.size() > 1 ? "s" : "");
        files.forEach(System.out::println);
    }

    private static void sortFiles(ArrayList<FileToMove> files) {
        Comparator<FileToMove> fileToMoveComparator = new Comparator<FileToMove>() {
            @Override
            public int compare(FileToMove o1, FileToMove o2) {
                if (o1.date.getTime() < o2.date.getTime()) {
                    return -1;
                } else if (o1.date.getTime() > o2.date.getTime()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        files.sort(fileToMoveComparator);
    }

    private interface FileInsideFolderFormatter {
        String format(FileToMove file);
    }

    private static String formatFile(FileToMove file, FileInsideFolderFormatter formatter, int part) {
        String partString = "";
        if (part != 0) {
            partString = " PART " + part;
        }
        return SYSTEM_DIRECTORY + "/" + file.destination + "/" + formatter.format(file) + partString + file.getFileExtension();
    }

    private static void moveFiles(ArrayList<FileToMove> files, FileInsideFolderFormatter formatter) {
        for (FileToMove file : files) {
            if (file.destination != null) {

                try {
                    String dest;
                    int count = 0;
                    do {
                        dest = formatFile(file, formatter, count++);
                    } while (new File(dest).exists());
                    new File(new File(dest).getParent()).mkdirs();
                    Files.move(
                            Paths.get(file.file.getAbsolutePath()),
                            Paths.get(dest));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void assignDestinations(ArrayList<Class> classes, ArrayList<FileToMove> files,
                                           double epsilonMinutes) {
        for (Class class_ : classes) {
            for (FileToMove file : files) {
                System.out.printf("\nIterating over class %s and file %s\n", class_.name, file.file.getName());
                Calendar cal = Calendar.getInstance();

                cal.setTime(file.date);
                //Not sure on the -1
                DayOfWeek dayOfWeekOfFile = DayOfWeek.of(cal.get(Calendar.DAY_OF_WEEK) - 1);
                boolean doesFileOccurOnDay = class_.daysOfWeek.contains(dayOfWeekOfFile);
                System.out.printf("Class %s %s on date of file\n", class_.name, doesFileOccurOnDay ? "occurs" : "does not occur");
                if (doesFileOccurOnDay) {
                    double difference = Math.abs(file.startTimeMinutes - class_.startTimeMinutes);
                    System.out.printf("Difference in minutes: %1.3f\n", difference);
                    if (difference <= epsilonMinutes) {
                        System.out.println("File located");
                        if (Double.isNaN(file.delta) || (epsilonMinutes < file.delta)) {
                            file.setDestination(class_.name);
                            file.setDelta(epsilonMinutes);
                        } else {
                            System.out.println("File already has destination");
                        }
                    }
                }
                System.out.println();
            }
        }
    }

    private static double dateToMinutes(Date date) {
        return date.getHours() * 60.0 + date.getMinutes() + (date.getSeconds() / 60.0);
    }

    private static Date getTodaysDate() {
        Date now = new Date();
        return new Date(now.getYear(), now.getMonth(), now.getDay());
    }

    private static void getFilesInFolder(String
                                                 configFileName, String inputFormat, ArrayList<String> validFileExtensions, ArrayList<FileToMove> files) {
        File dir = new File(SYSTEM_DIRECTORY + "/");
        String contents[] = dir.list();

        SimpleDateFormat dateFormat = new SimpleDateFormat(inputFormat);

        for (String fullFileName : contents) {
            File f = new File(fullFileName);
            if (f.isFile() && !(f.getName().matches(configFileName))) {
                String[] split = fullFileName.split("\\.");
                final String fileName = split[0];
                final String fileExtension = split[1];

                AtomicBoolean validFileExtension = new AtomicBoolean(false);
                validFileExtensions.forEach((v) -> {
                    if (fileExtension.toUpperCase().matches(v.toUpperCase())) {
                        validFileExtension.set(true);
                    }
                });


                if (validFileExtension.get()) {
                    try {
                        Date date = dateFormat.parse(fileName);
                        files.add(new FileToMove(date, f));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void readConfigFile(Scanner
                                               scanner, ArrayList<Class> classes, ArrayList<String> fileExtension, AtomicReference<Double> epsilonMinutes, AtomicReference<String> inputFormat, AtomicReference<String> outputFormat) {

        //First line is file extensions
        String line = scanner.nextLine();
        String[] split = line.split(",");
        fileExtension.addAll(Arrays.asList(split));

        //Second line is epsilon
        line = scanner.nextLine();
        epsilonMinutes.set(Double.parseDouble(line));

        //3rd line is input format
        inputFormat.set(scanner.nextLine());

        //4th line is output format
        outputFormat.set(scanner.nextLine());

        while (scanner.hasNext()) {
            line = scanner.nextLine();
//            System.out.println("line : " + line);
            split = line.split(",");
            String name = split[0];

            Date startDate = null;
            String[] dateSplit = split[1].split(":");
//            System.out.println("Date split: " + List.of(dateSplit).toString());
            try {
                startDate = new Date(0,
                        0,
                        0,
                        Integer.parseInt(dateSplit[0]),
                        Integer.parseInt(dateSplit[1]),
                        Integer.parseInt(dateSplit[2]));
            } catch (NumberFormatException num) {
                num.printStackTrace();
            }

            ArrayList<DayOfWeek> days = new ArrayList<>();
            for (int i = 2; i < split.length; i++) {
                final int splitAsInt = Integer.parseInt(split[i]);
                final DayOfWeek day = DayOfWeek.of(splitAsInt);
                days.add(day);
            }

            Class clasz = new Class(name, startDate, days);
            System.out.println("Adding class " + clasz.toString());
            classes.add(clasz);
        }
    }

    private static Scanner getScanner(String fileName) {
        File f = new File(fileName);
        if (!f.exists()) {
            System.out.println("Could not find Config.txt file");
            System.exit(1);
        }
        Scanner scanner = null;
        try {
            scanner = new Scanner(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return scanner;
    }

    static class FileToMove {

        private String destination = null;
        private final Date date;
        private final File file;
        private final double startTimeMinutes;
        private double delta = Double.NaN;

        public FileToMove(Date date, File file) {
            this.date = date;
            this.file = file;
            startTimeMinutes = dateToMinutes(date);
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public void setDelta(double delta) {
            this.delta = delta;
        }

        public String getFileExtension() {
            return "." + file.getAbsolutePath().split("\\.")[1];
        }

        @Override
        public String toString() {
            return "FileToMove{" +
                    "destination='" + destination + '\'' +
                    ", date=" + date +
                    ", file=" + file +
                    ", startTimeMinutes=" + startTimeMinutes +
                    ", delta=" + delta +
                    '}';
        }

        public String getDestination() {
            return destination;
        }
    }

    static class Class {
        private final String name;
        private final Date startTime;
        private final double startTimeMinutes;
        private final List<DayOfWeek> daysOfWeek;

        public Class(String name, Date startTime, List<DayOfWeek> dayOfWeek) {
            this.name = name;
            this.startTime = startTime;
            startTimeMinutes = dateToMinutes(startTime);
            this.daysOfWeek = dayOfWeek;
        }

        @Override
        public String toString() {
            return "Class{" +
                    "name='" + name + '\'' +
                    ", startTime=" + startTime +
                    ", startTimeMinutes=" + startTimeMinutes +
                    ", daysOfWeek=" + daysOfWeek +
                    '}';
        }
    }
}
