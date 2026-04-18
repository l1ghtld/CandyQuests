package ru.light.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Reward {
    
    private List<String> commands = new ArrayList<>();
}
