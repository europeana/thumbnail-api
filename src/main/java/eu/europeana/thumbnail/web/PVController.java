package eu.europeana.thumbnail.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
}
