package au.com.codeka.warworlds.common.sim;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Collection;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;

/**
 * Class for handling modifications to a star.
 */
public class StarModifier {
  private static final Log log = new Log("StarModifier");

  public interface IdentifierGenerator {
    long nextIdentifier();
  }

  private final IdentifierGenerator identifierGenerator;

  public StarModifier(IdentifierGenerator identifierGenerator) {
    this.identifierGenerator = identifierGenerator;
  }

  public void modifyStar(Star.Builder star, StarModification modification) {
    modifyStar(star, Lists.newArrayList(modification));
  }

  public void modifyStar(Star.Builder star, Collection<StarModification> modifications) {
    new Simulation(false).simulate(star);
    for (StarModification modification : modifications) {
      applyModification(star, modification);
    }
    new Simulation().simulate(star);
  }

  private void applyModification(Star.Builder star, StarModification modification) {
    switch (modification.type) {
      case COLONIZE:
        applyColonize(star, modification);
        return;
      case CREATE_FLEET:
        applyCreateFleet(star, modification);
        return;
      case ADJUST_FOCUS:
        applyAdjustFocus(star, modification);
        return;
      default:
        log.error("Unknown or unexpected modification type: %s", modification.type);
    }
  }

  private void applyColonize(Star.Builder star, StarModification modification) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.COLONIZE));

    star.planets.set(
        modification.planet_index,
        star.planets.get(modification.planet_index).newBuilder()
            .colony(new Colony.Builder()
                .cooldown_end_time(System.currentTimeMillis() + (15 * Time.MINUTE))
                .empire_id(modification.empire_id)
                .focus(new ColonyFocus.Builder()
                    .construction(0.1f)
                    .energy(0.3f)
                    .farming(0.3f)
                    .mining(0.3f)
                    .build())
                .id(identifierGenerator.nextIdentifier())
                .population(100.0f)
                .defence_bonus(1.0f)
                .build())
            .build());

    // if there's no storage for this empire, add one with some defaults now.
    boolean hasStorage = false;
    for (EmpireStorage storage : star.empire_stores) {
      if (storage.empire_id != null && storage.empire_id.equals(modification.empire_id)) {
        hasStorage = true;
      }
    }
    if (!hasStorage) {
      star.empire_stores.add(new EmpireStorage.Builder()
          .empire_id(modification.empire_id)
          .total_goods(100.0f).total_minerals(100.0f).total_energy(1000.0f)
          .max_goods(1000.0f).max_minerals(1000.0f).max_energy(1000.0f)
          .build());
    }
  }

  private void applyCreateFleet(Star.Builder star, StarModification modification) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.CREATE_FLEET));

    // TODO: simulate star
    star.fleets.add(new Fleet.Builder()
        //TODO: .alliance_id()
        .design_id(modification.design_id)
        .empire_id(modification.empire_id)
        .id(identifierGenerator.nextIdentifier())
        .num_ships((float) modification.count)
        .stance(Fleet.FLEET_STANCE.AGGRESSIVE)
        .state(Fleet.FLEET_STATE.IDLE)
        .state_start_time(System.currentTimeMillis())
        .build());
  }

  private void applyAdjustFocus(Star.Builder star, StarModification modification) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.ADJUST_FOCUS));

    for (int i = 0; i < star.planets.size(); i++) {
      Planet planet = star.planets.get(i);
      if (planet.colony != null && planet.colony.id.equals(modification.colony_id)) {
        star.planets.set(i, planet.newBuilder()
            .colony(planet.colony.newBuilder()
                .focus(modification.focus)
                .build())
            .build());
      }
    }
  }
}