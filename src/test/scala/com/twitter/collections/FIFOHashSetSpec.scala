package com.twitter.collections
import org.specs._

class FIFOHashSetSpec extends Specification {
  "FIFOHashSet" should {
    "store and retrieve an item" in {
      val set = new FIFOHashSet[Int]()
      set.add(1) must be_==(true)
      set.contains(1) must be_==(true)
    }

    "not grow over the size limit" in {
      val set = new FIFOHashSet[Int](1)
      set.add(1) must be_==(true)
      set.add(2) must be_==(true)
      set.size must be_==(1)
    }

    "evict the oldest item" in {
      val set = new FIFOHashSet[Int](10)
      for (i <- 1 to 10) {
        set.add(i)
      }
      for (i <- 11 to 100) {
        set.add(i)
        set.size must be_==(10)
        set.contains(i-10) must be_==(false)
        for (j <- i-9 to i) {
          set.contains(j) must be_==(true)
        }
      }
    }
  }
}
