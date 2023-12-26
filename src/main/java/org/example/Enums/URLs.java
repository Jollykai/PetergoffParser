package org.example.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum URLs {
    RUNS_PAGE("https://5verst.ru/petergofaleksandriysky/results/all/"),
    PAGE_WITH_RUN("https://5verst.ru/petergofaleksandriysky/results/{runDate}");

    private final String url;
}
