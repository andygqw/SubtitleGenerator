package org.example.Service;

import java.io.File;
import java.util.Optional;

public interface IResquestor {

    Optional<String> sendRequest(File file);
}
