package monix.kafka

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.execution.cancelables.BooleanCancelable
import monix.reactive.Consumer

import scala.concurrent.duration._

object CancellableObservableApp extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    val cancelable = BooleanCancelable()
    val observableTask =
      KafkaConsumerObservable[Array[Byte], Array[Byte]](KafkaConsumerConfig.default.copy(groupId = "test-group"), List("test-topic"))
        .takeWhileNotCanceled(cancelable)
        .consumeWith(Consumer.foreach(println))

    val cancelableTask = Task.eval {
      cancelable.cancel()
      println("canceled")
    }.delayExecution(5.seconds)

    Task.gatherUnordered(List(cancelableTask, observableTask)).map(_ => ExitCode.Success)
  }
}
