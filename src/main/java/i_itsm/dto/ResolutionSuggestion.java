package i_itsm.dto;

/** A finished incident that looks like the current one, and how it was fixed. */
public record ResolutionSuggestion(Long incidentId, String title, String category, String resolutionNotes, int matchPercent) {
}
