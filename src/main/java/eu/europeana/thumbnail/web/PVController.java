package eu.europeana.thumbnail.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/pv")
public class PVController {

    private static final Logger LOG = LogManager.getLogger(PVController.class);
    private static final String PV_FOLDER = "/mnt/pvdata";


    @RequestMapping("list")
    public Set<String> list() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(PV_FOLDER))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    @PostMapping("write/{fileName}")
    public String write(@PathVariable(name="fileName") String fileName,
                        @RequestBody String contents) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PV_FOLDER + "/" + fileName))) {
            writer.write(contents);
            LOG.info("Writing to file {}, contents = {}", fileName, contents);
            return "Write successful";
        }
    }

    @RequestMapping("read/{fileName}")
    public String read(@PathVariable(name="fileName") String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(PV_FOLDER + "/" + fileName))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    @DeleteMapping("delete/{fileName}")
    public String delete(@PathVariable(name="fileName") String fileName) {
        File file = new File(PV_FOLDER + "/" + fileName);
        file.delete();
        return "Delete successful";
    }
}
