package com.coreoz.play;

import akka.stream.Materializer;
import akka.stream.javadsl.AsPublisher;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import io.netty.buffer.Unpooled;
import play.libs.F;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;

/**
 * A body parser that do not parse the request,
 * but instead expose a <code>Publisher&lt;ByteBuf&gt;</code> object
 */
public class StreamingBodyParser extends BodyParser.Default {

	private final Materializer parserMaterializer;

	public StreamingBodyParser(Materializer parserMaterializer) {
		super(null, null, null);
		this.parserMaterializer = parserMaterializer;
	}

	@Override
	public Accumulator<ByteString, F.Either<Result, Object>> apply(Http.RequestHeader request) {
		if (request.hasBody()) {
			return Accumulator
				.<ByteString>source()
				.map(sourceBody -> F.Either.Right(
					sourceBody
						.map(bs -> Unpooled.wrappedBuffer(bs.toByteBuffer()))
						.runWith(Sink.asPublisher(AsPublisher.WITHOUT_FANOUT), parserMaterializer)
				), parserMaterializer.executionContext());
		} else {
			return BodyParser.<Optional<Void>, Object>widen(new Empty()).apply(request);
		}
	}
}
