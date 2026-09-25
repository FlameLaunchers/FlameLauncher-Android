package kr.co.donghyun.flamelauncher.presentation.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * [items] 를 최대 [limit] 개씩 겹쳐서 처리하고, **입력 순서 그대로** 결과를 돌려준다.
 *
 * 라이브러리·에셋은 작은 파일이 수십~수천 개라, 하나씩 받으면 파일 개수만큼 요청 왕복이
 * 그대로 쌓인다(대역폭이 아니라 왕복 횟수가 병목). 순서를 지키는 이유는 클래스패스처럼
 * 순서가 의미를 갖는 목록에도 그대로 쓰기 위해서다.
 */
fun <T, R> mapParallel(items: List<T>, limit: Int = 12, body: (T) -> R): List<R> =
    if (items.isEmpty()) emptyList() else runBlocking {
        val semaphore = Semaphore(limit)
        items.map { item ->
            async(Dispatchers.IO) { semaphore.withPermit { body(item) } }
        }.awaitAll()
    }
