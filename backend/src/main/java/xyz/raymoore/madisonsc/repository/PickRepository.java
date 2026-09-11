package xyz.raymoore.madisonsc.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import xyz.raymoore.madisonsc.domain.Pick;

@NullMarked
public interface PickRepository extends ListCrudRepository<Pick, UUID> {

    @Query("""
            SELECT pick_id, entry_date, contestant_id, year, week, team, underdog, line, result
            FROM madisonsc.pick
            ORDER BY year DESC, entry_date, pick_id
            """)
    List<Pick> findAllForStandings();

    @Query("""
            SELECT pick_id, entry_date, contestant_id, year, week, team, underdog, line, result
            FROM madisonsc.pick
            ORDER BY year DESC, week DESC
            LIMIT 1
            """)
    Optional<Pick> findMostRecent();

    @Query("""
            SELECT pick_id, entry_date, contestant_id, year, week, team, underdog, line, result
            FROM madisonsc.pick
            WHERE year = :year
            ORDER BY entry_date, pick_id
            """)
    List<Pick> findByYear(@Param("year") int year);

    @Query("""
            SELECT pick_id, entry_date, contestant_id, year, week, team, underdog, line, result
            FROM madisonsc.pick
            WHERE contestant_id = :contestantId
            ORDER BY year DESC, entry_date, pick_id
            """)
    List<Pick> findByContestantId(@Param("contestantId") UUID contestantId);

    @Query("SELECT DISTINCT year FROM madisonsc.pick ORDER BY year DESC")
    List<Integer> findAvailableYears();
}
