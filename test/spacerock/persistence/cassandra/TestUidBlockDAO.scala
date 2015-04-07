package spacerock.persistence.cassandra

import play.api.test.FakeApplication
import play.api.test.Helpers._

/**
 * Created by william on 3/1/15.
 */
object TestUidBlockDAO extends DaoTestModule {
  def main(args: Array[String]): Unit = {
    val app: FakeApplication = FakeApplication(withGlobal = Some(DaoTestGlobal))
    running(app) {
      val ub: UidBlock = inject [UidBlock]

      val l: scala.collection.mutable.Set[String] = new scala.collection.mutable.HashSet[String]
      l.add("1")
      l.add("2")
      l.add("3")
      l.add("4")
      l.add("5")
      l.add("6")
      assert(ub.addNewBlock(1, l.toSet, false, -1))
      l.clear()
      l.add("7")
      l.add("8")
      l.add("9")
      l.add("10")
      assert(ub.addNewBlock(2, l.toSet, false, -1))
      l.clear()
      l.add("10")
      l.add("11")
      l.add("12")
      l.add("13")
      assert(ub.addNewBlock(3, l.toSet, false, -1))
      l.clear()
      l.add("14")
      l.add("16")
      l.add("30")
      l.add("22")
      assert(ub.addNewBlock(4, l.toSet, false, -1))

      println(ub.assignBlockToServer(2, 1))
      println(ub.assignBlockToServer(2, 1))
      var n: Int = ub.getNextBlockId()
      println(n)
      println(ub.assignBlockToServer(n, 1))
      n = ub.getNextBlockId()
      println(n)
      println(ub.assignBlockToServer(n, 1))
      println(ub.getNextBlockId())
      println(ub.freeBlock(2))

    }
  }
}
