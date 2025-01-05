

// //import com.atakmap.android.apolloedge.MyTestClass
import org.junit.Test
import org.junit.Assert.*
import com.atakmap.android.apolloedge.MyTestClass

 /**
  * Example local unit test, which will execute on the development machine (host).
  *
  * See [testing documentation](http://d.android.com/tools/testing).
  */
 class ExampleUnitTest {
     @Test
     fun addition_isCorrect() {
         assertEquals(4, 2 + 2)
     }
     @Test
     fun testMyClass() {
         val myObj = MyTestClass()
         myObj.printMe()
         assert(myObj.getResult() == 1)
     }
 }
