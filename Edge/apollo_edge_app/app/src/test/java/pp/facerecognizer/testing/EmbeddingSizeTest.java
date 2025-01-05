package pp.facerecognizer.testing;

// Example local unit test

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import pp.facerecognizer.Classifier;

public class EmbeddingSizeTest {

    @Test
    public void embeddingSizeReasonablyLarge() {
        assertTrue(Classifier.EMBEDDING_SIZE > 8);

    }
}
