package jakarta.faces.event;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.*;

@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface AfterPhase
{
    PhaseId value();

    public final static class Literal extends AnnotationLiteral<AfterPhase> implements AfterPhase
    {
        private static final long serialVersionUID = 1L;

        private final PhaseId value;

        public static Literal of(PhaseId value)
        {
            return new Literal(value);
        }

        private Literal(PhaseId value)
        {
            this.value = value;
        }

        @Override
        public PhaseId value()
        {
            return value;
        }
    }
}