package com.ouroboros.ants.exec;

import com.ouroboros.ants.utils.Input;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class ConsoleStrategyExecutor extends AbstractStrategyExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleStrategyExecutor.class);

    @Override
    public Input update(List<String> states) {
        StringBuilder line = new StringBuilder();
        int c;
        try {
            while ((c = System.in.read()) >= 0) {
                switch (c) {
                    case '\n':
                    case '\r':
                        if (line.length() > 0) {
                            String fullLine = line.toString();
//                            LOGGER.debug("input {}", fullLine);
                            switch (fullLine) {
                                case Input.readyStr:
                                    return Input.ready;
                                case Input.goStr:
                                    return Input.go;
                                default:
                                    states.add(fullLine);
                            }
                            line = new StringBuilder();
                        }
                        break;
                    default:
                        line.append((char)c);
                        break;
                }
            }
            return Input.end;

        } catch (IOException e) {
//           LOGGER.error("failed to read input with exception", e);
        }

//       LOGGER.error("failed to read input, input never ends");

        return null;
    }

    @Override
    public void finish() {
        LOGGER.debug("output turn");

        System.out.println("go");
        System.out.flush();
    }

    @Override
    public void move(Move move) {
        LOGGER.debug("output move {}", move);

        System.out.println("o " + move.y + " " + move.x + " " + move.d);
        System.out.flush();
    }
}
