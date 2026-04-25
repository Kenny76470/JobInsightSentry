package com.job.hunter.service;

import java.io.IOException;

public interface AiService {
    String analyze(String text) throws IOException;
}