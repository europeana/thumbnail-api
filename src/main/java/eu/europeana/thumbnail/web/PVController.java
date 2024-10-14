package eu.europeana.thumbnail.web;

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
    public String write(@RequestParam String fileName, @RequestBody String contents) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PV_FOLDER + "/" + fileName))) {
            writer.write(contents);
            return "Write successful";
        }
    }

    @RequestMapping("read/{fileName}")
    public String read(@RequestParam String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(PV_FOLDER + "/" + fileName))) {
            return read(fileName);
        }
    }

    @DeleteMapping("delete/{fileName}")
    public String delete(@RequestParam String fileName) {
        File file = new File(PV_FOLDER + "/" + fileName);
        file.delete();
        return "Delete successful";
    }
}
