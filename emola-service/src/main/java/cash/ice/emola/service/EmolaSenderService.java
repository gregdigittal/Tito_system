package cash.ice.emola.service;

import cash.ice.emola.dto.EmolaRequest;
import cash.ice.emola.dto.EmolaResponse;

public interface EmolaSenderService {

    EmolaResponse sendRequest(EmolaRequest request);
}
