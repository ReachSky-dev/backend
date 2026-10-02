package pl.reachsky.backend.identity.application;

import org.junit.jupiter.api.Test;
import pl.reachsky.backend.identity.application.port.out.UserProfileRepository;
import pl.reachsky.backend.shared.MissingSubjectException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnsureUserProfileServiceUnitTest {

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final EnsureUserProfileService service = new EnsureUserProfileService(repository);

    @Test
    void ensureWithNullSubject_throwsMissingSubjectException() {
        assertThatThrownBy(() -> service.ensure(null, "Alice"))
                .isInstanceOf(MissingSubjectException.class);
        verify(repository, never()).findBySubject(any());
    }

    @Test
    void ensureWithBlankSubject_throwsMissingSubjectException() {
        assertThatThrownBy(() -> service.ensure("   ", "Alice"))
                .isInstanceOf(MissingSubjectException.class);
        verify(repository, never()).findBySubject(any());
    }

    @Test
    void ensureWithValidSubject_delegatesToRepository() {
        when(repository.findBySubject("sub-123")).thenReturn(Optional.empty());

        service.ensure("sub-123", "Alice");

        verify(repository).findBySubject("sub-123");
    }
}
