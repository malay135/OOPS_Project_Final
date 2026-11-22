package i_itsm.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import i_itsm.dto.ResolutionSuggestion;
import i_itsm.model.Incident;
import i_itsm.repository.IncidentRepository;

/**
 * Suggested resolution (intelligent feature): finds finished incidents that look like this one
 * and shows how they were fixed, best match first.
 */
@Service
public class ResolutionSuggestionService {

    static final double MIN_SCORE = 0.2; // below this the incidents are not really alike
    static final int MAX_SUGGESTIONS = 3;

    private final IncidentRepository incidentRepository;

    public ResolutionSuggestionService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public List<ResolutionSuggestion> suggestFor(Long incidentId) {
        Incident current = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        record Scored(Incident incident, double score) { }

        return incidentRepository.findAll().stream()
                .filter(i -> !i.getId().equals(current.getId()))
                .filter(i -> "RESOLVED".equals(i.getStatus()) || "CLOSED".equals(i.getStatus()))
                .filter(i -> i.getResolutionNotes() != null && !i.getResolutionNotes().isBlank())
                .map(i -> new Scored(i, score(current, i)))
                .filter(s -> s.score() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(MAX_SUGGESTIONS)
                .map(s -> new ResolutionSuggestion(s.incident().getId(), s.incident().getTitle(), s.incident().getCategory(),
                        s.incident().getResolutionNotes(), (int) Math.round(Math.min(1, s.score()) * 100)))
                .toList();
    }

    /** Title counts most, then description; the same category adds a small bonus. */
    static double score(Incident a, Incident b) {
        double score = 0.6 * TextSimilarity.similarity(a.getTitle(), b.getTitle())
                + 0.4 * TextSimilarity.similarity(a.getDescription(), b.getDescription());
        if (Objects.equals(a.getCategory(), b.getCategory())) score += 0.1;
        return score;
    }
}
