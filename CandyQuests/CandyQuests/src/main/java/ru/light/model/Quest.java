package ru.light.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import ru.light.model.enums.QuestType;

@Getter
@RequiredArgsConstructor
public class Quest {
    
    private final int id;
    private final QuestType type;
    private final Material material;
    private final EntityType entityType;
    private final int needed;
    private final int level;
}
