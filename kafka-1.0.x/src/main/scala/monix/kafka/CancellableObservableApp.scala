package monix.kafka

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.execution.cancelables.BooleanCancelable
import monix.reactive.Consumer

import scala.concurrent.duration._

object CancellableObservableApp extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    val cancelable = BooleanCancelable()
    val observableTask = KafkaConsumerObservable
      .apply[Array[Byte], Array[Byte]](KafkaConsumerConfig.default.copy(groupId = "consumer-group"), List("test-topic"))
      .doAfterSubscribe(Task.eval(println("After subscribe")))
      .bufferTimedWithPressure(1.second, 1)
      .takeWhileNotCanceled(cancelable)
      .consumeWith(Consumer.foreach(_ => ()))

    val cancelTask = Task.eval {
      cancelable.cancel()
      println("Should stop now")
    }.delayExecution(8.seconds).delayResult(8.seconds)

    Task.parZip2(cancelTask, observableTask).map(_ => ExitCode.Success)
  }
}
