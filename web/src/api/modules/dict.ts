import request from '../request'
import type { Result } from '@/types'

export interface DictItem {
  itemKey: string
  itemValue: string
  extra: string
  sortOrder: number
}

export const dictApi = {
  getItems(typeCode: string) {
    return request.get<any, Result<DictItem[]>>(`/dicts/${typeCode}`)
  }
}
