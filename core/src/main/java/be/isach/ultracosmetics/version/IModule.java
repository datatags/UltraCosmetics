package be.isach.ultracosmetics.version;

import be.isach.ultracosmetics.cosmetics.morphs.Morph;
import be.isach.ultracosmetics.cosmetics.mounts.Mount;
import be.isach.ultracosmetics.cosmetics.pets.Pet;

public interface IModule {
    public boolean enable();

    public void disable();

    public Class<? extends Mount> getSpiderClass();

    public Class<? extends Mount> getSlimeClass();

    Class<? extends Pet> getPumplingClass();

    Class<? extends Morph> getElderGuardianClass();
}
